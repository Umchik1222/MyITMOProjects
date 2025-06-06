package request.requestWorkers;
import exceptions.UnsupportedRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import request.requests.ServerRequest;
import requests.*;

import java.util.LinkedHashMap;
import java.util.Optional;


public class RequestWorkerManager {

    private static final Logger logger = LogManager.getLogger("RequestWorkerManager");

    private final LinkedHashMap<Class<?>, RequestWorker> workers = new LinkedHashMap<>();

    public RequestWorkerManager() {
        workers.put(AbsRequest.class, new BaseRequestWorker());
        workers.put(CommandClientRequest.class, new CommandClientRequestWorker());
        workers.put(ArgumentCommandClientRequest.class, new ArgumentCommandClientRequestWorker<>());
        workers.put(CommandDescriptionsRequest.class, new CommandConfigureRequestWorker());
        workers.put(AuthRequest.class, new AuthWorker());
        workers.put(RegRequest.class, new RegWorker());
    }

    public void workWithRequest(ServerRequest request) {
        try {
            Optional.ofNullable(workers.get(request.getUserRequest().getClass())).orElseThrow(() -> new UnsupportedRequestException("Указанный запрос не может быть обработан")).workWithRequest(request);
        } catch (UnsupportedRequestException ex) {
            logger.error("Получен невалидный запрос.");
        }
    }
}
