package lk.lumina.library.web;

import jakarta.validation.Valid;
import java.util.List;

import lk.lumina.library.model.FeedbackItem;
import lk.lumina.library.model.FeedbackPriority;
import lk.lumina.library.model.FeedbackStatus;
import lk.lumina.library.model.FeedbackType;

import lk.lumina.library.service.FeedbackBusinessException;
import lk.lumina.library.service.FeedbackService;

import lk.lumina.library.web.form.FeedbackCaseUpdateForm;
import lk.lumina.library.web.form.FeedbackReplyForm;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/feedback/manage")
@PreAuthorize("hasRole('LIBRARY_MANAGER')")
public class ManagerFeedbackController {

    private static final String RETRY_HINT = " Please retry the action.";

    // WITHDRAWN is excluded because only a member can withdraw a case.
    private static final List<FeedbackStatus> MANAGER_STATUSES = List.of(
            FeedbackStatus.OPEN,
            FeedbackStatus.IN_REVIEW,
            FeedbackStatus.RESOLVED,
            FeedbackStatus.CLOSED);

    private final FeedbackService feedbackService;

    public ManagerFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String showManagerPage(
            @RequestParam(required = false) FeedbackType type,
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackPriority priority,
            @RequestParam(required = false) String keyword,
            Model model) {

        String cleanKeyword = keyword == null ? "" : keyword.trim();
        List<FeedbackItem> items = feedbackService.searchForManager(
                type, status, priority, cleanKeyword);

        addManagerPageData(model, items, type, status, priority, cleanKeyword);
        return "feedback-manage";
    }

    @PostMapping("/{id}/reply")
    public String replyToCase(
            @PathVariable Long id,
            @Valid @ModelAttribute("replyForm") FeedbackReplyForm replyForm,
            BindingResult errors,
            @RequestParam(name = "filterType", required = false) FeedbackType filterType,
            @RequestParam(name = "filterStatus", required = false) FeedbackStatus filterStatus,
            @RequestParam(name = "filterPriority", required = false) FeedbackPriority filterPriority,
            @RequestParam(name = "filterKeyword", required = false) String filterKeyword,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            rememberRetryDraft(flash, id, replyForm.getMessage());
            flash.addFlashAttribute("error", firstError(errors) + RETRY_HINT);
            return managerRedirect(flash, filterType, filterStatus, filterPriority, filterKeyword);
        }

        try {
            feedbackService.replyByManager(id, replyForm.getMessage());
            flash.addFlashAttribute("success", "Reply sent and the member was notified.");
        } catch (FeedbackBusinessException exception) {
            rememberRetryDraft(flash, id, replyForm.getMessage());
            flash.addFlashAttribute("error", exception.getMessage() + RETRY_HINT);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            rememberRetryDraft(flash, id, replyForm.getMessage());
            flash.addFlashAttribute("error", "The reply could not be sent." + RETRY_HINT);
        }

        return managerRedirect(flash, filterType, filterStatus, filterPriority, filterKeyword);
    }

    @PostMapping("/{id}/status")
    public String updateCase(
            @PathVariable Long id,
            @Valid @ModelAttribute("caseUpdateForm") FeedbackCaseUpdateForm caseUpdateForm,
            BindingResult errors,
            @RequestParam(name = "filterType", required = false) FeedbackType filterType,
            @RequestParam(name = "filterStatus", required = false) FeedbackStatus filterStatus,
            @RequestParam(name = "filterPriority", required = false) FeedbackPriority filterPriority,
            @RequestParam(name = "filterKeyword", required = false) String filterKeyword,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            flash.addFlashAttribute("error", firstError(errors) + RETRY_HINT);
            return managerRedirect(flash, filterType, filterStatus, filterPriority, filterKeyword);
        }

        try {
            // Validate the status transition before updating the case.
            feedbackService.updateCase(id, caseUpdateForm);
            flash.addFlashAttribute("success", "Case status and priority updated.");
        } catch (FeedbackBusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage() + RETRY_HINT);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            flash.addFlashAttribute(
                    "error",
                    "The case could not be updated." + RETRY_HINT);
        }

        return managerRedirect(flash, filterType, filterStatus, filterPriority, filterKeyword);
    }

    @PostMapping("/{id}/archive")
    public String archiveCase(
            @PathVariable Long id,
            @RequestParam(name = "filterType", required = false) FeedbackType filterType,
            @RequestParam(name = "filterStatus", required = false) FeedbackStatus filterStatus,
            @RequestParam(name = "filterPriority", required = false) FeedbackPriority filterPriority,
            @RequestParam(name = "filterKeyword", required = false) String filterKeyword,
            RedirectAttributes flash) {

        try {
            // Archive the case without deleting its conversation history.
            feedbackService.archive(id);
            flash.addFlashAttribute("success", "Case archived.");
        } catch (FeedbackBusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage() + RETRY_HINT);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", "The case could not be archived." + RETRY_HINT);
        }

        return managerRedirect(flash, filterType, filterStatus, filterPriority, filterKeyword);
    }

    private void addManagerPageData(
            Model model,
            List<FeedbackItem> items,
            FeedbackType selectedType,
            FeedbackStatus selectedStatus,
            FeedbackPriority selectedPriority,
            String keyword) {

        model.addAttribute("items", items);
        model.addAttribute("replies", feedbackService.repliesFor(items));
        model.addAttribute("stats", feedbackService.stats());
        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("statuses", FeedbackStatus.values());
        model.addAttribute("managerStatuses", MANAGER_STATUSES);
        model.addAttribute("priorities", FeedbackPriority.values());
        model.addAttribute("selectedType", selectedType);
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("selectedPriority", selectedPriority);
        model.addAttribute("keyword", keyword);
        if (!model.containsAttribute("replyForm")) {
            model.addAttribute("replyForm", new FeedbackReplyForm());
        }
    }

    // Keep the reply text after a failed submit so the manager can retry.
    private void rememberRetryDraft(RedirectAttributes flash, Long caseId, String message) {
        flash.addFlashAttribute("retryCaseId", caseId);
        flash.addFlashAttribute("retryMessage", message);
    }

    private String firstError(BindingResult errors) {
        if (errors.getAllErrors().isEmpty()) {
            return "Please check the submitted values.";
        }
        return errors.getAllErrors().get(0).getDefaultMessage();
    }

    private String managerRedirect(
            RedirectAttributes flash,
            FeedbackType type,
            FeedbackStatus status,
            FeedbackPriority priority,
            String keyword) {

        if (type != null) {
            flash.addAttribute("type", type);
        }
        if (status != null) {
            flash.addAttribute("status", status);
        }
        if (priority != null) {
            flash.addAttribute("priority", priority);
        }
        if (keyword != null && !keyword.isBlank()) {
            flash.addAttribute("keyword", keyword.trim());
        }
        return "redirect:/feedback/manage";
    }
}
