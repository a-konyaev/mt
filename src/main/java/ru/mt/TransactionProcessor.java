package ru.mt;

/**
 * Обработчик транзакций по переводу денег.
 * фактически реализует бизнес-логику перевода денег.
 *
 *   - работает с таблицей transaction, у которой след. колонки:
 *     - id (PK) - timeUUID! - чтобы можно было "отсекать" старые транзакции для повышения эффективности select-ов
 *     - account_from
 *     - account_to
 *     - amount
 *     - status
 *     - timastamp
 *
 *    Алгоритм:
 *     - взять запрос из очереди на "посмотерть", т.е. не извлекать пока окончательно.
 *     - далее проходим стадии, которые соотв. статусам транзакции
 *      (важно! эта обработка идемпотентна, т.е. если обработчик упал при обработке запроса из очереди,
 *      то после рестарта он снова начнет выполнять этот же запрос)
 *
 *     CREATED (новая зарегистрированная транзакция):
 *       проверяем, есть в табл. transaction есть запись с таким же id
 *       - если есть, идем в стадию, соотв. статусу, который сейчас прописан у запроса
 *       - если нет, добавляет новую запись в transaction со статусом STARTED и перейти к этой стадии
 *
 *     STARTED (выполнение транзакции началось):
 *       - AccountService: зарезервируй у account_from сумму amount
 *       - если ответ OK, то обновить статус на RESERVED и перейти к этой стадии
 *       - если ответ DENIED или CANCELED, то обновить статус на DENIED и перейти к этой стадии
 *       - иначе (ответ DEBITED) - какой то сбой, т.е. деньги по транзакции уже списаны со счета account_from,
 *          но статус транзакции такой, что мы еще не добрались до увеличения счета account_to,
 *          поэтому обновить статус на ERROR и перейти к этой стадии.
 *
 *     RESERVED (деньги зарезервированы у счета From):
 *       - AccountService: добавить сумму amount на счет account_to
 *       - если ответ OK, то обновить статус на ADDED и перейти к этой стадии
 *       - если ответ ERROR, то обновить статус на CANCELLING и перейти к этой стадии
 *
 *     ADDED (деньги добавлены на счет To):
 *       - AccountService: списать ранее зарезервированную сумму amount со счета account_to
 *       - если ответ OK, то обновить статус на DONE и перейти к этой стадии
 *       - если ответ ERROR, то обновить статус на ERROR и перейти к этой стадии
 *
 *     CANCELLING (отмена транзации из-за невозможности ее выполнить, но не из-за ошибки):
 *       - AccountService: отменить резервирование суммы amount на счете account_to
 *       - если ответ OK, то обновить статус на DENIED и перейти к этой стадии
 *       - если ответ ERROR, то обновить статус на ERROR и перейти к этой стадии
 *
 *     DONE (ранее зарезервированные деньги списаны со счета From):
 *     DENIED (в операции отказано, например из-за нехватки денег на счете From):
 *     ERROR (завершено с ошибкой, требуется тех. поддержка):
 *       - завершение обработки события - коммит в очереди для текущего запроса, переход к следующему запросу.
 */
public class TransactionProcessor {
    public String registerPutMoneyTransaction(String accountId, double amount) {
        return null;
    }

    public String registerwithdrawMoneyTransaction(String accountId, double amount) {
        return null;
    }

    /**
     * Зарегистрировать новую транзакцию
     * @param accountIdFrom
     * @param accountIdTo
     * @param amount
     * @return ИД зарегистрированной транзакции
     */
    public String registerTransferMoneyTransaction(String accountIdFrom, String accountIdTo, double amount) {
        return null;
    }

    public TransactionStatus getTransactionStatus(String transactionId) {
        /*
        - найти запись: select from transaction where id = ИД транзакции and макс. дата
        - если нашли, то вернуть ее статус
        - если не нашли, то вернуть статус CREATED
         */
        return TransactionStatus.CREATED;
    }
}
