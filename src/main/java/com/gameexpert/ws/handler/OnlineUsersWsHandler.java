package com.gameexpert.ws.handler;


import com.gameexpert.api.SessionRegistry;
import com.gameexpert.ws.NicknameHandshakeInterceptor;
import com.gameexpert.ws.WorldBroadcaster;
import com.gameexpert.ws.WorldSessionRegistry;
import com.gameexpert.ws.WsMessageContext;
import com.gameexpert.ws.dto.OnlineUsersResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUsersWsHandler implements WsMessageHandler {
    private final WorldSessionRegistry registry;
    private final WorldBroadcaster broadcaster;

    @Override
    public String type() {
        return "onlineUsers";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        // TODO Lv 15: 현재 월드의 열린 연결에서 닉네임을 조회하고 요청자에게 응답합니다.
        List<SessionRegistry.Entry> entries = registry.entries(context.worldId()).stream()
                .filter(entry -> entry.session().isOpen()) //isOpen()이 true인 세션만 선별
                .toList();

        List<String> users = entries.stream()
                .map(SessionRegistry.Entry::session)
                .map(session -> (String) session.getAttributes().get(NicknameHandshakeInterceptor.ATTR_NICKNAME))
                .sorted()
                .toList();

        OnlineUsersResponse onlineUsersResponse = new OnlineUsersResponse(users, users.size());

        broadcaster.sendTo(context.session(), onlineUsersResponse);
    }
}
