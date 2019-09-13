package ru.mt;

import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;
import ru.mt.domain.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/*
  Отвечает за управление балансом своей пачки счетов, т.е. только этот AccountBalanceManager имеет доступ,
  причем синхронный, к счетам, за который отвечает.

  Все вызовы обрабатывает через запросы, которые добавляются в табл. account_balance_call.
  При этом обрабатывает только те запросы, которые относятся к его пачке счетов.

  Алгоритм получения новых запросов:
    (*) можно сделать очередь, в которую тот, кто создает новые запросы, будет публиковать событие с ИД запроса,
    а AccountBalanceManager-ы слушают эту очередь, и аналогично через очередь нотифицируют о том, что запрос обработан.

    - периодически получает из БД запросы для его пачки счетов, которые еще не обработаны
        и упорядоченные по возрастанию времени (т.е. первыми идут самые старые):
      select from account_balance_call
      where <ИД счета> in (пачка счетов данного AccountBalanceManager) and <результат выполнения> is null
      order by timestamp desc
    - последовательно обрабатывает запросы, причем внутри одной БД-транзакции с проставлением статуса запроса.

  работает с таблицами в БД:
    - account - хранит текущий баланс счета (см. ru.mt.domain.Account), колонки:
      - ИД счета (PK)
      - сумма
    - account_balance_reverved - записи о резервировании средств на счете, колонки:
      - ИД счета (PK)
      - ИД транзакции (которая стала причиной резервирования)
      - timastamp (? нужно ли)
      - сумма резервирования
      - статус - см. ReservationStatus
 */
@Log4j2
public class AccountBalanceManager
        extends Component {

    private final int shardIndex;
    private final AccountBalanceCallRepository balanceCallRepo;
    private final Thread callProcessingThread;

    public AccountBalanceManager(int shardIndex) {
        this.shardIndex = shardIndex;
        balanceCallRepo = Configuration.getComponent(AccountBalanceCallRepository.class);

        callProcessingThread = new Thread(this::processCalls, String.format("abm-%04d", shardIndex));
        callProcessingThread.start();
    }

    @Override
    public void destroy() {
        if (callProcessingThread != null) {
            callProcessingThread.interrupt();
            try {
                callProcessingThread.join(1000);
            } catch (InterruptedException e) {
                log.error("Error while interruption callProcessingThread", e);
            }
        }
    }

    private void processCalls() {
        log.debug("starting work...");

        while (!Thread.interrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

            log.debug("getting next call...");
            var call = balanceCallRepo.getNextCall(shardIndex);
            if (call == null)
                continue;

            executeCall(call);
        }

        log.debug("work had interrupted");
    }

    private void executeCall(AccountBalanceCall call) {
        log.debug("executing call: " + call);
        var callId = call.getId();
        var resultBuilder = AccountBalanceCallResult.builder().callId(callId);
        try {
            //todo:...

            var result = resultBuilder.build();
            log.debug("setting call result: " + result);
            balanceCallRepo.setCallResult(callId, result);
        }
        catch (Throwable e) {
            resultBuilder.errorMessage(e.getMessage());
            var result = resultBuilder.build();
            log.error("call execution failed: " + result);
            balanceCallRepo.setCallResult(callId, result);
        }
    }

    //region operation with account balance

    /**
     * получить баланс счета
     *
     * @param accountId
     * @return доступная сумма на балансе с учетом всех зарезервированных средств
     */
    private double getBalance(String accountId) {
        /*
        - сумма зарезервированных = сумму денег для всех записей в account_balance_reverved с заданным ИД счета, у которых статус ОК
        - доступная сумма = <значение баланса из табл. account> - <сумма зарезервированных>
         */
        return 0;
    }

    /**
     * Зарезервировать деньги на счете
     *
     * @param accountId     ИД счета
     * @param transactionId ИД транзакции, в рамках которой выполнить резервирование
     * @param amount        сумма денег
     * @return статус резервирования
     */
    private ReservationStatus reserveAmount(String accountId, String transactionId, double amount) {
        /*
        - (*) если время ИД транзакции больше максимального (помним, что transactionId - это timeUUID), то возвращаем ошибку.
            это нужно для оптимизации работы с таблицей, чтобы не перелопачивать все записи.

        - если резервирования с таким ИД еще нет, то:
          - получим сумму на счете с учетом всех зарезервированных денег
          - если эта сумма >= чем сумма, кот. надо зарезервировать, то
            - создаем новую запись в account_balance_reserved с заданной суммой резервирования и со статусом ОК
            - возвращаем ОК
          - иначе возвращаем DENIED (отказано, т.к. не хватает денег)
        - если уже есть резервирование с таким ИД запроса, то возвращает его статус (OK, DEBITED или CANCELED)
         */
        return ReservationStatus.OK;
    }

    /**
     * списать ранее зарезервированную сумму со счета
     *
     * @param accountId
     * @param transactionId
     * @return статус OK/ERROR
     */
    private boolean debitReservedAmount(String accountId, String transactionId) {
        /*
        - если статус резервирования с заданным ИД != OK, то возвращаем ошибку
        - иначе (статус == OK), в одной транзакции в БД:
          - уменьшаем баланс счета в account
          - выставляем статус для резервирования = DEBITED
         */
        return true;
    }

    /**
     * отменить ранее созданное резервирование суммы
     *
     * @param accountId
     * @param transactionId
     * @return статус OK/ERROR
     */
    private boolean cancelReservedAmount(String accountId, String transactionId) {
        /*
        - если статус резервирования с заданным ИД != OK, то возвращаем ошибку
        - иначе - устанавливаем статус резервирования = CANCELED
         */
        return true;
    }

    /**
     * добавить сумму на счет
     *
     * @param accountId
     * @param transactionId
     * @param amount
     * @return статус OK/ERROR
     */
    private boolean addAmount(String accountId, String transactionId, double amount) {
        /*
        - увеличиваем баланс счета в табл. account
        - (*) если реализовать функцию блокировки счета, то можно вернуть ошибку.
         */
        return true;
    }

    //endregion
}
