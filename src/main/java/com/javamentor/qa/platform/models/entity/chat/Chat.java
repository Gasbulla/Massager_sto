package com.javamentor.qa.platform.models.entity.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(generator = "Chat_seq")
    private Long id;

    @Column
    private String title;

    @Column
    private String image;

    @Column(name = "persist_date", updatable = false)
    @Type(type = "org.hibernate.type.LocalDateTimeType")
    @CreationTimestamp
    private LocalDateTime persistDate;

    @Enumerated
    @Column(columnDefinition = "int2")
    private ChatType chatType;


    public Chat(ChatType chatType) {
        this.chatType = chatType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return Objects.equals(id, chat.id) && Objects.equals(title, chat.title) &&
                Objects.equals(persistDate, chat.persistDate) && chatType == chat.chatType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, persistDate, chatType);
    }
}
