package com.mockarena.identity.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="users", schema="identity")
public class IdentityUser {
 @Id private UUID id; @Column(nullable=false) private String email; @Column(nullable=false) private String displayName; @Column(nullable=false) private String passwordHash; @Enumerated(EnumType.STRING) @Column(nullable=false) private UserStatus status;
 @ElementCollection(targetClass=UserRole.class, fetch=FetchType.EAGER) @CollectionTable(name="user_roles", schema="identity", joinColumns=@JoinColumn(name="user_id")) @Column(name="role_code") @Enumerated(EnumType.STRING) private Set<UserRole> roles=new HashSet<>();
 @Version private long version; @Column(nullable=false,updatable=false) private Instant createdAt; @Column(nullable=false) private Instant updatedAt;
 protected IdentityUser(){}
 public IdentityUser(UUID id,String email,String displayName,String passwordHash,Instant now){this.id=id;this.email=email;this.displayName=displayName;this.passwordHash=passwordHash;this.status=UserStatus.ACTIVE;this.roles.add(UserRole.USER);this.createdAt=now;this.updatedAt=now;}
 public UUID id(){return id;} public String email(){return email;} public String displayName(){return displayName;} public String passwordHash(){return passwordHash;} public UserStatus status(){return status;} public Set<UserRole> roles(){return Set.copyOf(roles);} public long version(){return version;}
 public void grant(UserRole role,Instant now){roles.add(role);updatedAt=now;}
 @Override public String toString(){return "IdentityUser[id="+id+", email="+email+", status="+status+"]";}
}
