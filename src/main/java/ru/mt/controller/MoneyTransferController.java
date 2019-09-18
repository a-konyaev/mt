package ru.mt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.log4j.Log4j2;
import ru.mt.MoneyTransferService;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.controller.dto.*;
import ru.mt.errors.MoneyTransferException;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Реализует рест-апи и внутри вызывает MoneyTransferService
 */
@Log4j2
public class MoneyTransferController extends Component {
    private final MoneyTransferService moneyTransferService;
    private final ObjectMapper objectMapper;
    private HttpServer httpServer;


    public MoneyTransferController() throws IOException {
        moneyTransferService = Configuration.getComponent(MoneyTransferService.class);
        objectMapper = new ObjectMapper();
        initHttpServer();
    }

    @Override
    protected void destroyInternal() {
        httpServer.stop(1);
    }

    private void initHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(8081), 0);

        httpServer.createContext("/api/list", exg -> handler(exg, this::getAccountsHandler));
        httpServer.createContext("/api/new", exg -> handler(exg, this::createNewAccountHandler));
        httpServer.createContext("/api/balance", exg -> handler(exg, this::getAccountBalanceHandler));
        httpServer.createContext("/api/put", exg -> handler(exg, this::putMoneyIntoAccountHandler));
        httpServer.createContext("/api/withdraw", exg -> handler(exg, this::withdrawMoneyFromAccountHandler));
        httpServer.createContext("/api/transfer", exg -> handler(exg, this::transferMoneyHandler));

        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
    }

    interface RequestHandler {
        MoneyTransferResponse handle(QueryParams params) throws MoneyTransferException, QueryParamsException;
    }

    private void handler(HttpExchange exchange, RequestHandler requestHandler) throws IOException {
        var requestURI = exchange.getRequestURI();
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                // Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            var queryParams = QueryParams.fromRawQuery(requestURI.getRawQuery());

            MoneyTransferResponse response;
            int respCode;
            try {
                response = requestHandler.handle(queryParams);
                respCode = 200; // OK
            } catch (QueryParamsException e) {
                response = new ErrorResponse(e.toString());
                respCode = 400; // Bad Request
            } catch (MoneyTransferException e) {
                response = new ErrorResponse(e.toString());
                respCode = 404; // Not Found
            }

            log.info(String.format("%s --> [%s] %s", requestURI, respCode, response));

            var bytes = objectMapper.writeValueAsBytes(response);
            exchange.sendResponseHeaders(respCode, bytes.length);

            OutputStream output = exchange.getResponseBody();
            output.write(bytes);
            output.flush();

        } catch (Throwable e) {
            log.error("Request handling failed: " + requestURI, e);
            // Server Error
            exchange.sendResponseHeaders(500, -1);

        } finally {
            exchange.close();
        }
    }

    //region handlers

    private MoneyTransferResponse getAccountsHandler(QueryParams params) {
        var accounts = moneyTransferService.getAccounts();
        return new AccountIdsResponse(accounts);
    }

    private MoneyTransferResponse createNewAccountHandler(QueryParams params) {
        var accountId = moneyTransferService.createNewAccount();
        return new AccountBalanceResponse(accountId, BigDecimal.ZERO);
    }

    private MoneyTransferResponse getAccountBalanceHandler(QueryParams params)
            throws MoneyTransferException, QueryParamsException {

        var accountId = params.getParamString("accountId");
        var balance = moneyTransferService.getAccountBalance(accountId);

        return new AccountBalanceResponse(accountId, balance);
    }

    private MoneyTransferResponse putMoneyIntoAccountHandler(QueryParams params)
            throws MoneyTransferException, QueryParamsException {

        var accountId = params.getParamString("accountId");
        var amount = params.getParamBigDecimal("amount");
        moneyTransferService.putMoneyIntoAccount(accountId, amount);

        return new OKResponse();
    }

    private MoneyTransferResponse withdrawMoneyFromAccountHandler(QueryParams params)
            throws MoneyTransferException, QueryParamsException {

        var accountId = params.getParamString("accountId");
        var amount = params.getParamBigDecimal("amount");
        moneyTransferService.withdrawMoneyFromAccount(accountId, amount);

        return new OKResponse();
    }

    private MoneyTransferResponse transferMoneyHandler(QueryParams params)
            throws MoneyTransferException, QueryParamsException {

        var accountIdFrom = params.getParamString("accountIdFrom");
        var accountIdTo = params.getParamString("accountIdTo");
        var amount = params.getParamBigDecimal("amount");
        moneyTransferService.transferMoney(accountIdFrom, accountIdTo, amount);

        return new OKResponse();
    }

    //endregion
}
