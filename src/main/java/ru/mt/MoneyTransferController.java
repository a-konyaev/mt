package ru.mt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.utils.ApiUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Реализует рест-апи и внутри вызывает MoneyTransferService
 */
@Log4j2
public class MoneyTransferController extends Component {
    private final MoneyTransferService moneyTransferService;
    private HttpServer httpServer;

    public MoneyTransferController() throws IOException {
        moneyTransferService = Configuration.getComponent(MoneyTransferService.class);

        initHttpServer();
    }

    @Override
    protected void destroyInternal() {
        httpServer.stop(1);
    }

    private void initHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(8080), 0);

        httpServer.createContext("/api/hello", exg -> handler(exg, this::helloHandler));

        httpServer.createContext("/api/list", exg -> handler(exg, this::getAccountsHandler));
        httpServer.createContext("/api/new", exg -> handler(exg, this::createNewAccountHandler));
        httpServer.createContext("/api/balance", exg -> handler(exg, this::getAccountBalanceHandler));
        httpServer.createContext("/api/put", exg -> handler(exg, this::putMoneyIntoAccountHandler));
        httpServer.createContext("/api/withdraw", exg -> handler(exg, this::withdrawMoneyFromAccountHandler));
        httpServer.createContext("/api/transfer", exg -> handler(exg, this::transferMoneyHandler));

        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
    }

    interface ProcessRequest {
        String process(Map<String, List<String>> params);
    }

    private void handler(HttpExchange exchange, ProcessRequest processRequest) throws IOException {
        if (!checkMethodGet(exchange))
            return;

        Map<String, List<String>> params = ApiUtils.splitQuery(exchange.getRequestURI().getRawQuery());
        log.info("new request: " + params);

        var response = processRequest.process(params);
        sendResponse(exchange, response);
    }

    private String helloHandler(Map<String, List<String>> params) {
        return LocalDateTime.now().toString() + ": Preved medved!";
    }

    private String getAccountsHandler(Map<String, List<String>> params) {
        var accounts = moneyTransferService.getAccounts();
        return accounts.toString();
    }

    private String createNewAccountHandler(Map<String, List<String>> params) {
        var newAccountId = moneyTransferService.createNewAccount();
        return newAccountId;
    }

    private String getAccountBalanceHandler(Map<String, List<String>> params) {
        return null;
    }

    private String putMoneyIntoAccountHandler(Map<String, List<String>> params) {
        return null;
    }

    private String withdrawMoneyFromAccountHandler(Map<String, List<String>> params) {
        return null;
    }

    private String transferMoneyHandler(Map<String, List<String>> params) {
        return null;
    }

    private boolean checkMethodGet(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            return true;
        }

        exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
        return false;
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        if (response == null || response.isEmpty()) {
            exchange.sendResponseHeaders(200, 0);
        } else {
            var bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream output = exchange.getResponseBody();
            output.write(bytes);
            output.flush();
        }

        exchange.close();
    }
}
