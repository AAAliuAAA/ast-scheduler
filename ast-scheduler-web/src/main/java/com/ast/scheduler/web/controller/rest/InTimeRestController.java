package com.ast.scheduler.web.controller.rest;

import com.ast.scheduler.core.service.AgentStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class InTimeRestController {


    @Autowired
    private AgentStreamService openClawStreamService;

    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam(required = false) String message) {

        return openClawStreamService.streamOpenClaw("","hi","main","")
                .filter(content -> !content.isEmpty())
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build());
    }

}
