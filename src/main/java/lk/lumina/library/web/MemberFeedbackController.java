package lk.lumina.library.web;

import jakarta.validation.Valid;
import java.util.List;

import lk.lumina.library.model.FeedbackItem;
import lk.lumina.library.model.FeedbackType;

import lk.lumina.library.service.FeedbackBusinessException;
import lk.lumina.library.service.FeedbackService;

import lk.lumina.library.web.form.FeedbackForm;
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

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/feedback")
@PreAuthorize("hasRole('MEMBER')")
public class MemberFeedbackController {

    private static final String MEMBER_PAGE = "redirect:/feedback";
    private static final String RETRY_HINT = " Please retry the action.";

    private final FeedbackService feedbackService;

    public MemberFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String showFeedbackPage(Model model) {
        if (!model.containsAttribute("feedbackForm")) {
            model.addAttribute("feedbackForm", new FeedbackForm());
        }
        addMemberPageData(model);
        return "feedback";
    }

    @PostMapping
    public String submitCase(
            @Valid @ModelAttribute("feedbackForm") FeedbackForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            addMemberPageData(model);
            return "feedback";
        }

        try {
            FeedbackItem item = feedbackService.submit(form);
            flash.addFlashAttribute(
                    "success",
                    "Your message was submitted. Reference: " + item.getReferenceNumber());
        } catch (FeedbackBusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage() + RETRY_HINT);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            flash.addFlashAttribute(
                    "error",
                    "Your case could not be submitted." + RETRY_HINT);
        }

        return MEMBER_PAGE;
    }

    @GetMapping("/{id}/edit")
    public String showEditPage(
            @PathVariable Long id,
            Model model,
            RedirectAttributes flash) {

        // Verify that the case belongs to the logged-in member.
        FeedbackItem item = feedbackService.currentMemberItem(id);

        try {
            feedbackService.assertMemberCanEdit(item);
        } catch (FeedbackBusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
            return MEMBER_PAGE;
        }

        addEditPageData(model, item, FeedbackForm.from(item));
        return "feedback-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateCase(
            @PathVariable Long id,
            @Valid @ModelAttribute("feedbackForm") FeedbackForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        FeedbackItem item = feedbackService.currentMemberItem(id);

        if (errors.hasErrors()) {
            addEditPageData(model, item, form);
            return "feedback-edit";
        }

        try {
            feedbackService.updateByMember(id, form);
            flash.addFlashAttribute("success", "Feedback case updated successfully.");
        } catch (FeedbackBusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage() + RETRY_HINT);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", "The case could not be updated." + RETRY_HINT);
        }

        return MEMBER_PAGE;
    }

    @PostMapping("/{id}/withdraw")
    public String withdrawCase(@PathVariable Long id, RedirectAttributes flash) {
        try {
            // Withdraw keeps the case and conversation; it does not delete history.
            feedbackService.withdrawByMember(id);
            flash.addFlashAttribute("success", "Feedback case withdrawn.");
        } catch (FeedbackBusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage() + RETRY_HINT);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", "The case could not be withdrawn." + RETRY_HINT);
        }

        return MEMBER_PAGE;
    }

    @PostMapping("/{id}/reply")
    public String replyToCase(
            @PathVariable Long id,
            @Valid @ModelAttribute("replyForm") FeedbackReplyForm replyForm,
            BindingResult errors,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            rememberRetryDraft(flash, id, replyForm.getMessage());
            flash.addFlashAttribute("error", firstError(errors) + RETRY_HINT);
            return MEMBER_PAGE;
        }

        try {
            feedbackService.replyByMember(id, replyForm.getMessage());
            flash.addFlashAttribute("success", "Your reply was added to the case.");
        } catch (FeedbackBusinessException exception) {
            rememberRetryDraft(flash, id, replyForm.getMessage());
            flash.addFlashAttribute("error", exception.getMessage() + RETRY_HINT);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            rememberRetryDraft(flash, id, replyForm.getMessage());
            flash.addFlashAttribute("error", "Your reply could not be sent." + RETRY_HINT);
        }

        return MEMBER_PAGE;
    }

    private void addMemberPageData(Model model) {
        List<FeedbackItem> items = feedbackService.currentMemberItems();
        model.addAttribute("items", items);
        model.addAttribute("replies", feedbackService.repliesFor(items));
        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("categories", feedbackService.categories());
        if (!model.containsAttribute("replyForm")) {
            model.addAttribute("replyForm", new FeedbackReplyForm());
        }
    }

    private void addEditPageData(Model model, FeedbackItem item, FeedbackForm form) {
        model.addAttribute("item", item);
        model.addAttribute("feedbackForm", form);
        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("categories", feedbackService.categories());
    }

    // Keep the reply text after a failed submit so the member can retry.
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
}
