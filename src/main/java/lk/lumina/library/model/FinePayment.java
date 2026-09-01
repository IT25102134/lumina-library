package lk.lumina.library.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="fine_payments")
public class FinePayment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(optional=false) @JoinColumn(name="loan_id",unique=true) private Loan loan;
    @ManyToOne(optional=false) @JoinColumn(name="member_id") private UserAccount member;
    @Column(nullable=false,precision=10,scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentStatus status=PaymentStatus.UNPAID;
    private LocalDateTime paidAt; @Column(length=80) private String reference;
    public FinePayment(){}
    public Long getId(){return id;} public Loan getLoan(){return loan;} public void setLoan(Loan v){loan=v;}
    public UserAccount getMember(){return member;} public void setMember(UserAccount v){member=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public PaymentStatus getStatus(){return status;} public void setStatus(PaymentStatus v){status=v;}
    public LocalDateTime getPaidAt(){return paidAt;} public void setPaidAt(LocalDateTime v){paidAt=v;}
    public String getReference(){return reference;} public void setReference(String v){reference=v;}
}
