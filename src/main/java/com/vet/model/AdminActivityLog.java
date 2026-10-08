package com.vet.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="admin_activity_logs")
public class AdminActivityLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne @JoinColumn(name="user_id") private User user;
    @Column(nullable=false) private String action;
    @Column(length=2000) private String description;
    @Column(nullable=false) private LocalDateTime eventTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public User getUser(){return user;} public void setUser(User v){user=v;}
    public String getAction(){return action;} public void setAction(String v){action=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public LocalDateTime getEventTime(){return eventTime;} public void setEventTime(LocalDateTime v){eventTime=v;}
}
