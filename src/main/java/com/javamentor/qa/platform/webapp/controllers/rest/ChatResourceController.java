package com.javamentor.qa.platform.webapp.controllers.rest;

import com.javamentor.qa.platform.dao.impl.pagination.messagedto.MessagePageDtoByGroupChatId;
import com.javamentor.qa.platform.models.dto.GroupChatDto;
import com.javamentor.qa.platform.models.entity.pagination.PaginationData;
import com.javamentor.qa.platform.service.abstracts.dto.ChatDtoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/user/chat")
@Tag(name="Chat Resource Controller", description = "Управление сущностями, которые связаны с чатами.")
public class ChatResourceController {
    private final ChatDtoService chatDtoService;

    public ChatResourceController(ChatDtoService chatDtoService) {
        this.chatDtoService = chatDtoService;
    }

    @GetMapping("/group/{groupChatId}")
    @Operation (summary = "Получение группового чата с сообщениями.", description = "Получение группового чата с пагинированным списком сообщений. " +
            "Внимание! Передаваемый id описывает именно групповой чат: id чата может отличаться от соответствуюшего ему id группового чата.")
    @ApiResponse (responseCode = "200", description = "Групповой чат найден", content ={
            @Content(mediaType = "application/json"),
    })
    @ApiResponse (responseCode = "400", description = "Групповой чат с указанными id не найден" , content ={
            @Content(mediaType = "application/json"),
    })

    public ResponseEntity<GroupChatDto> getGroupChatDtoById(
            @PathVariable("groupChatId")
            @Parameter(name = "Id группового чата.", required = true, description = "Id группового чата является обязательным параметром." +
                    "Внимание! Передаваемый id описывает именно групповой чат: id чата может отличаться от соответствуюшего ему id группового чата.")
                    long groupChatId,
            @RequestParam(name = "itemsOnPage", defaultValue = "10")
            @Parameter (name = "Количество сообщений на странице.",
                    description = "Необязательный параметр. Позволяет настроить количество сообщений на одной странице. По-умолчанию равен 10.")
                    int itemsOnPage,
            @RequestParam(name = "currentPage", defaultValue = "1")
            @Parameter (hidden = true, name = "Текущая пагинированная страница сообщений.",
                    description = "Необязательный параметр. Служит для корректного постраничного отображения сообщений и обращения к ним. " +
                            "Параметр технический, потому скрыт. По-умолчанию равен 1")
                    int currentPage) {
        PaginationData properties = new PaginationData(currentPage, itemsOnPage, MessagePageDtoByGroupChatId.class.getSimpleName());
        properties.getProps().put("groupChatId", groupChatId);
        if (chatDtoService.getGroupChatDtoById(groupChatId, properties).isPresent()) {
            return new ResponseEntity<>(chatDtoService.getGroupChatDtoById(groupChatId, properties).get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

    }
}
