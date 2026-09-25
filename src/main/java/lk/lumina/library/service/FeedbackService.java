package lk.lumina.library.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lk.lumina.library.model.FeedbackItem;
import lk.lumina.library.model.FeedbackPriority;
import lk.lumina.library.model.FeedbackReply;
import lk.lumina.library.model.FeedbackStatus;
import lk.lumina.library.model.FeedbackType;
import lk.lumina.library.model.Notification;
import lk.lumina.library.model.NotificationType;
import lk.lumina.library.model.Role;
import lk.lumina.library.model.UserAccount;

import lk.lumina.library.repository.FeedbackReplyRepository;
import lk.lumina.library.repository.FeedbackRepository;
import lk.lumina.library.repository.NotificationRepository;
import lk.lumina.library.repository.UserAccountRepository;

import lk.lumina.library.web.form.FeedbackCaseUpdateForm;
import lk.lumina.library.web.form.FeedbackForm;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Handles the business rules of the feedback and complaint module. */
@Service
@Transactional
public class FeedbackService {

    private static final int MIN_REPLY_LENGTH = 5;
    private static final int MAX_REPLY_LENGTH = 2500;

    private static final Set<FeedbackStatus> ARCHIVABLE_STATUSES =
            Set.of(FeedbackStatus.CLOSED, FeedbackStatus.WITHDRAWN);

    private static final List<String> CATEGORIES = List.of(
            "Library Service", "Book Collection", "Staff Support",
            "Facilities", "Digital Resources", "Events and Programs", "Other");

    private final FeedbackRepository feedbackRepository;
    private final FeedbackReplyRepository replyRepository;
    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            FeedbackReplyRepository replyRepository,
            NotificationRepository notificationRepository,
            UserAccountRepository userRepository,
            CurrentUserService currentUserService,
            AuditService auditService) {

        this.feedbackRepository = feedbackRepository;
        this.replyRepository = replyRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return CATEGORIES;
    }

    @Transactional(readOnly = true)
    public List<FeedbackItem> currentMemberItems() {
        Long memberId = currentUserService.get().getId();
        return feedbackRepository
                .findByMemberIdAndArchivedFalseOrderByCreatedAtDesc(memberId);
    }

    /** The member ID in the query prevents access to another member's case. */
    @Transactional(readOnly = true)
    public FeedbackItem currentMemberItem(Long id) {
        UserAccount member = currentUserService.get();
        return feedbackRepository
                .findByIdAndMemberIdAndArchivedFalse(id, member.getId())
                .orElseThrow(this::caseNotFound);
    }

    public FeedbackItem submit(FeedbackForm form) {
        UserAccount member = currentUserService.get();
        FeedbackItem item = new FeedbackItem();

        item.setMember(member);
        copyForm(item, form);
        item = feedbackRepository.save(item);

        audit(member, "SUBMIT", item, item.getSubject());
        notifyManagers(
                "New feedback case",
                member.getFullName() + " submitted " + item.getReferenceNumber() + ".");

        return item;
    }

    public void updateByMember(Long id, FeedbackForm form) {
        FeedbackItem item = currentMemberItem(id);
        assertMemberCanEdit(item);
        copyForm(item, form);
        audit(currentUserService.get(), "EDIT", item, item.getSubject());
    }

    /** A member can edit only an OPEN case with no replies. */
    @Transactional(readOnly = true)
    public void assertMemberCanEdit(FeedbackItem item) {
        boolean reviewStarted = item.getStatus() != FeedbackStatus.OPEN;
        boolean conversationStarted =
                replyRepository.existsByFeedbackItemId(item.getId());

        if (reviewStarted || conversationStarted) {
            throw new FeedbackBusinessException(
                    "This case can no longer be edited because review has started.");
        }
    }

    public void withdrawByMember(Long id) {
        FeedbackItem item = currentMemberItem(id);

        if (!item.getStatus().isMemberCanWithdraw()) {
            throw new FeedbackBusinessException(
                    "A resolved or closed case cannot be withdrawn.");
        }

        item.setStatus(FeedbackStatus.WITHDRAWN);
        item.setResolvedAt(LocalDateTime.now());

        UserAccount member = currentUserService.get();
        audit(member, "WITHDRAW", item, item.getReferenceNumber());
        notifyManagers(
                "Feedback case withdrawn",
                member.getFullName() + " withdrew " + item.getReferenceNumber() + ".");
    }

    public void replyByMember(Long id, String message) {
        FeedbackItem item = currentMemberItem(id);
        assertRepliesAllowed(item);

        UserAccount member = currentUserService.get();
        saveReply(item, member, message);

        // A member reply reopens a resolved case.
        if (item.getStatus() == FeedbackStatus.RESOLVED) {
            item.setStatus(FeedbackStatus.IN_REVIEW);
            item.setResolvedAt(null);
        }

        audit(member, "REPLY", item, item.getReferenceNumber());
        notifyManagers(
                "Member replied",
                member.getFullName() + " replied to " + item.getReferenceNumber() + ".");
    }

    @Transactional(readOnly = true)
    public List<FeedbackItem> searchForManager(
            FeedbackType type,
            FeedbackStatus status,
            FeedbackPriority priority,
            String keyword) {

        String cleanKeyword =
                keyword == null || keyword.isBlank() ? null : keyword.trim();

        return feedbackRepository.searchForManager(
                type, status, priority, cleanKeyword);
    }

    @Transactional(readOnly = true)
    public FeedbackItem managerItem(Long id) {
        return feedbackRepository
                .findByIdAndArchivedFalse(id)
                .orElseThrow(this::caseNotFound);
    }

    public void replyByManager(Long id, String message) {
        FeedbackItem item = managerItem(id);
        assertRepliesAllowed(item);

        UserAccount manager = currentUserService.get();
        saveReply(item, manager, message);

        // The first manager reply starts the review.
        if (item.getStatus() == FeedbackStatus.OPEN) {
            item.setStatus(FeedbackStatus.IN_REVIEW);
        }

        notifyMember(
                item,
                "Feedback case updated",
                "The library manager replied to " + item.getReferenceNumber() + ".");
        audit(manager, "RESPOND", item, item.getReferenceNumber());
    }

    public void updateCase(Long id, FeedbackCaseUpdateForm form) {
        validateCaseUpdateForm(form);

        FeedbackItem item = managerItem(id);
        FeedbackStatus oldStatus = item.getStatus();
        FeedbackPriority oldPriority = item.getPriority();
        FeedbackStatus newStatus = form.getStatus();
        FeedbackPriority newPriority = form.getPriority();

        validateTransition(oldStatus, newStatus);

        boolean statusChanged = oldStatus != newStatus;
        boolean priorityChanged = oldPriority != newPriority;
        if (!statusChanged && !priorityChanged) {
            return;
        }

        item.setStatus(newStatus);
        item.setPriority(newPriority);

        // A priority-only change must not replace the resolution time.
        if (statusChanged) {
            updateCompletionTime(item, newStatus);
        }

        audit(
                currentUserService.get(),
                "UPDATE",
                item,
                newStatus + " / " + newPriority);

        notifyMember(
                item,
                "Feedback status updated",
                item.getReferenceNumber()
                        + " is now " + newStatus.getDisplayName()
                        + " with " + newPriority.getDisplayName() + " priority.");
    }

    public void archive(Long id) {
        FeedbackItem item = managerItem(id);

        if (!ARCHIVABLE_STATUSES.contains(item.getStatus())) {
            throw new FeedbackBusinessException(
                    "Close or withdraw the case before archiving it.");
        }

        // Archiving keeps the history but removes the case from active lists.
        item.setArchived(true);
        audit(
                currentUserService.get(),
                "ARCHIVE",
                item,
                item.getReferenceNumber());
    }

    /** Loads all replies in one query and groups them by case ID. */
    @Transactional(readOnly = true)
    public Map<Long, List<FeedbackReply>> repliesFor(List<FeedbackItem> items) {
        Map<Long, List<FeedbackReply>> replies = new LinkedHashMap<>();

        for (FeedbackItem item : items) {
            replies.put(item.getId(), new ArrayList<>());
        }

        if (items.isEmpty()) {
            return replies;
        }

        List<Long> feedbackIds =
                items.stream().map(FeedbackItem::getId).toList();

        for (FeedbackReply reply :
                replyRepository.findConversationReplies(feedbackIds)) {

            replies.get(reply.getFeedbackItem().getId()).add(reply);
        }

        return replies;
    }

    @Transactional(readOnly = true)
    public FeedbackStats stats() {
        return new FeedbackStats(
                feedbackRepository.countByArchivedFalse(),
                feedbackRepository.countByStatusAndArchivedFalse(FeedbackStatus.OPEN),
                feedbackRepository.countByStatusAndArchivedFalse(FeedbackStatus.IN_REVIEW),
                feedbackRepository.countByStatusAndArchivedFalse(FeedbackStatus.RESOLVED),
                feedbackRepository.countByTypeAndArchivedFalse(FeedbackType.COMPLAINT));
    }

    private void copyForm(FeedbackItem item, FeedbackForm form) {
        item.setType(form.getType());
        item.setCategory(clean(form.getCategory()));
        item.setSubject(clean(form.getSubject()));
        item.setMessage(clean(form.getMessage()));
    }

    private void saveReply(
            FeedbackItem item,
            UserAccount author,
            String message) {

        String cleanMessage = validateAndCleanReply(message);
        replyRepository.save(new FeedbackReply(item, author, cleanMessage));
    }

    private void assertRepliesAllowed(FeedbackItem item) {
        if (!item.getStatus().isRepliesAllowed()) {
            throw new FeedbackBusinessException(
                    "A closed or withdrawn case cannot receive new replies.");
        }
    }

    private void validateTransition(
            FeedbackStatus current,
            FeedbackStatus next) {

        if (!current.canManagerTransitionTo(next)) {
            throw new FeedbackBusinessException(
                    "Invalid status change from " + current + " to " + next + ".");
        }
    }

    private void validateCaseUpdateForm(FeedbackCaseUpdateForm form) {
        if (form == null
                || form.getStatus() == null
                || form.getPriority() == null) {

            throw new FeedbackBusinessException(
                    "Status and priority are required.");
        }
    }

    /** Sets the first completion time or clears it when a case is reopened. */
    private void updateCompletionTime(
            FeedbackItem item,
            FeedbackStatus newStatus) {

        if (!newStatus.isCompleted()) {
            item.setResolvedAt(null);
        } else if (item.getResolvedAt() == null) {
            item.setResolvedAt(LocalDateTime.now());
        }
    }

    private void notifyMember(
            FeedbackItem item,
            String title,
            String message) {

        notificationRepository.save(new Notification(
                item.getMember(), title, message, NotificationType.INFO));
    }

    /** Notifications are sent only to active manager accounts. */
    private void notifyManagers(String title, String message) {
        for (UserAccount manager :
                userRepository.findByRole(Role.LIBRARY_MANAGER)) {

            if (manager.isActive()) {
                notificationRepository.save(new Notification(
                        manager, title, message, NotificationType.INFO));
            }
        }
    }

    /** Service validation also protects callers that do not use the web form. */
    private String validateAndCleanReply(String message) {
        String cleanMessage = clean(message);
        boolean validLength =
                cleanMessage.length() >= MIN_REPLY_LENGTH
                        && cleanMessage.length() <= MAX_REPLY_LENGTH;
        boolean containsLetter =
                cleanMessage.codePoints().anyMatch(Character::isLetter);

        if (!validLength || !containsLetter) {
            throw new FeedbackBusinessException(
                    "Reply must contain 5 to 2500 characters and include a letter.");
        }

        return cleanMessage;
    }

    private void audit(
            UserAccount actor,
            String action,
            FeedbackItem item,
            String details) {

        auditService.record(
                actor.getEmail(),
                action,
                "Feedback",
                item.getId(),
                details);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseStatusException caseNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Feedback case not found");
    }
}
