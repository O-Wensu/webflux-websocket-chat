package com.example.webfluxwebsocketchat.handler;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@Slf4j
public class CustomWebSocketHandler implements WebSocketHandler {

    private final Sinks.Many<String> sink;

    public CustomWebSocketHandler(Sinks.Many<String> sink) {
        this.sink = sink;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var output = session.receive()
                // 메시지를 JSON 객체로 변환
                .map(e -> e.getPayloadAsText())
                .map(e -> {
                    try {
                        // 메시지를 파싱
                        JSONObject json = new JSONObject(e);
                        String username = json.getString("username");
                        if (username.equals("")) username = "익명";
                        String message = json.getString("message");
                        return username + ": " + message;
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                        return "메시지 처리 중 오류 발생";
                    }
                });

        output.subscribe(s -> sink.emitNext(s, Sinks.EmitFailureHandler.FAIL_FAST));

        return session.send(sink.asFlux().map(session::textMessage));
    }
}
