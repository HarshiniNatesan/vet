package com.vet.model;

import javax.persistence.*;

@Entity
@Table(name="users")
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true)
    private String username;
    @Column(nullable=false)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role;
    private String fullName;
    private String email;
    private String phone;

    public User() {}
    public User(String username, String password, Role role, String fullName, String email, String phone) {
        this.username=username; this.password=password; this.role=role; this.fullName=fullName; this.email=email; this.phone=phone;
    }
    public Long getId(){return id;} public String getUsername(){return username;} public String getPassword(){return password;}
    public Role getRole(){return role;} public String getFullName(){return fullName;} public String getEmail(){return email;} public String getPhone(){return phone;}
    public void setId(Long id){this.id=id;} public void setUsername(String v){username=v;} public void setPassword(String v){password=v;}
    public void setRole(Role v){role=v;} public void setFullName(String v){fullName=v;} public void setEmail(String v){email=v;} public void setPhone(String v){phone=v;}
}
