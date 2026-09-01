package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "loans")
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="member_id") private UserAccount member;
    @ManyToOne(optional=false) @JoinColumn(name="copy_id") private BookCopy copy;
    @Column(nullable=false) private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime issuedAt; private LocalDateTime dueAt; private LocalDateTime returnedAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private LoanStatus status=LoanStatus.REQUESTED;
    @Column(nullable=false) private int renewalCount=0;
    public Loan(){} public Loan(UserAccount member, BookCopy copy){this.member=member;this.copy=copy;}
    public Long getId(){return id;} public UserAccount getMember(){return member;} public void setMember(UserAccount v){member=v;}
    public BookCopy getCopy(){return copy;} public void setCopy(BookCopy v){copy=v;}
    public LocalDateTime getRequestedAt(){return requestedAt;} public LocalDateTime getIssuedAt(){return issuedAt;} public void setIssuedAt(LocalDateTime v){issuedAt=v;}
    public LocalDateTime getDueAt(){return dueAt;} public void setDueAt(LocalDateTime v){dueAt=v;}
    public LocalDateTime getReturnedAt(){return returnedAt;} public void setReturnedAt(LocalDateTime v){returnedAt=v;}
    public LoanStatus getStatus(){return status;} public void setStatus(LoanStatus v){status=v;}
    public int getRenewalCount(){return renewalCount;} public void setRenewalCount(int v){renewalCount=v;}
}
