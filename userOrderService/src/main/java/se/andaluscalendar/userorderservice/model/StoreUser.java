package se.andaluscalendar.userorderservice.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_user")
@Data
public class StoreUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String role;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
