package se.andaluscalendar.userorderservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_user")
@Data
public class StoreUser {
    @Id
    private UUID id;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String role;
    private LocalDateTime createdAt;
}
