package lk.lumina.library.web;

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lk.lumina.library.model.FeedbackItem;
import lk.lumina.library.model.FeedbackPriority;
import lk.lumina.library.model.FeedbackStatus;
import lk.lumina.library.model.FeedbackType;

import lk.lumina.library.service.FeedbackBusinessException;
import lk.lumina.library.service.FeedbackService;

import lk.lumina.library.web.form.FeedbackCaseUpdateForm;
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
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Uses the existing login/session and delegates feedback rules
 * to FeedbackService.
 */
@Controller
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // Display the member page with a new form and the member's cases.
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/feedback")
    public String member(
            @RequestParam(name = "new", required = false) FeedbackType newType,
            Model model) {

        FeedbackForm form = new FeedbackForm();
        if (newType != null) {
            form.setType(newType);
        }
        model.addAttribute("feedbackForm", form);
        model.addAttribute("showNewForm", newType != null);
        populateMemberPage(model);

        return "feedback";
    }

    // Validate and submit a new case.
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/feedback")
    public String submit(
            @Valid @ModelAttribute("feedbackForm") FeedbackForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        // BindingResult must immediately follow the validated form.
        if (!errors.hasErrors()) {
            try {
                FeedbackItem item = feedbackService.submit(form);

                flash.addFlashAttribute(
                        "success",
                        "Your case was submitted: "
                                + item.getReferenceNumber()
                );

                return "redirect:/feedback";

            } catch (FeedbackBusinessException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }

        // Keep invalid input visible so the member can correct it.
        model.addAttribute("showNewForm", true);
        populateMemberPage(model);

        return "feedback";
    }

    // Load an existing case into the edit form.
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/feedback/{id}/edit")
    public String edit(
            @PathVariable Long id,
            Model model,
            RedirectAttributes flash) {

        FeedbackItem item = feedbackService.currentMemberItem(id);

        try {
            feedbackService.assertMemberCanEdit(item);

        } catch (FeedbackBusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/feedback";
        }

        model.addAttribute(
                "feedbackForm",
                FeedbackForm.from(item)
        );

        populateEditPage(model, item);

        return "feedback-edit";
    }

    // Validate and save changes to the member's case.
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/feedback/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("feedbackForm") FeedbackForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        // Resolve ownership before rendering or saving an edit.
        FeedbackItem item = feedbackService.currentMemberItem(id);

        try {
            feedbackService.assertMemberCanEdit(item);

        } catch (FeedbackBusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/feedback";
        }

        if (!errors.hasErrors()) {
            try {
                feedbackService.updateByMember(id, form);

                flash.addFlashAttribute(
                        "success",
                        "Your case was updated."
                );

                return "redirect:/feedback";

            } catch (FeedbackBusinessException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }

        populateEditPage(model, item);

        return "feedback-edit";
    }

    // Withdraw a case while preserving its history.
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/feedback/{id}/withdraw")
    public String withdraw(
            @PathVariable Long id,
            RedirectAttributes flash) {

        try {
            feedbackService.withdrawByMember(id);

            flash.addFlashAttribute(
                    "success",
                    "Your case was withdrawn. Its history is retained."
            );

        } catch (FeedbackBusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/feedback";
    }

    // Members can complete their own case directly without a reply workflow.
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/feedback/{id}/close")
    public String closeByMember(
            @PathVariable Long id,
            RedirectAttributes flash) {

        try {
            feedbackService.closeByMember(id);
            flash.addFlashAttribute("success", "Your case has been closed.");
        } catch (FeedbackBusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/feedback";
    }

    // Validate and save a member's follow-up reply.
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/feedback/{id}/reply")
    public String memberReply(
            @PathVariable Long id,
            @Valid @ModelAttribute("replyForm") FeedbackReplyForm form,
            BindingResult errors,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {

            keepReply(
                    flash,
                    id,
                    form.getMessage(),
                    validationMessage(errors)
            );

        } else {
            try {
                feedbackService.replyByMember(id, form.getMessage());

                flash.addFlashAttribute(
                        "success",
                        "Your reply was sent."
                );

            } catch (FeedbackBusinessException ex) {
                keepReply(
                        flash,
                        id,
                        form.getMessage(),
                        ex.getMessage()
                );
            }
        }

        return "redirect:/feedback";
    }

    // Display the manager queue, filters, conversations, and statistics.
    @PreAuthorize("hasRole('LIBRARY_MANAGER')")
    @GetMapping("/feedback/manage")
    public String manage(
            @RequestParam(required = false) FeedbackType type,
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackPriority priority,
            @RequestParam(required = false) String keyword,
            Model model) {

        List<FeedbackItem> items =
                feedbackService.searchForManager(
                        type,
                        status,
                        priority,
                        keyword
                );

        model.addAttribute("items", items);
        model.addAttribute("replies", feedbackService.repliesFor(items));
        model.addAttribute("stats", feedbackService.stats());

        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("statuses", FeedbackStatus.values());
        model.addAttribute("priorities", FeedbackPriority.values());

        // WITHDRAWN is reserved for the member's withdrawal action.
        model.addAttribute(
                "managerStatuses",
                Arrays.stream(FeedbackStatus.values())
                        .filter(value -> value != FeedbackStatus.WITHDRAWN)
                        .toList()
        );

        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("keyword", keyword);

        return "feedback-manage";
    }

    // Validate and save the manager's reply.
    @PreAuthorize("hasRole('LIBRARY_MANAGER')")
    @PostMapping("/feedback/manage/{id}/reply")
    public String managerReply(
            @PathVariable Long id,
            @Valid @ModelAttribute("replyForm") FeedbackReplyForm form,
            BindingResult errors,
            @RequestParam Map<String, String> parameters,
            RedirectAttributes flash) {

        keepFilters(parameters, flash);

        if (errors.hasErrors()) {

            keepReply(
                    flash,
                    id,
                    form.getMessage(),
                    validationMessage(errors)
            );

        } else {
            try {
                feedbackService.replyByManager(id, form.getMessage());

                flash.addFlashAttribute(
                        "success",
                        "Reply sent and member notified."
                );

            } catch (FeedbackBusinessException ex) {
                keepReply(
                        flash,
                        id,
                        form.getMessage(),
                        ex.getMessage()
                );
            }
        }

        return "redirect:/feedback/manage";
    }

    // Apply a valid status transition and priority selection.
    @PreAuthorize("hasRole('LIBRARY_MANAGER')")
    @PostMapping("/feedback/manage/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @Valid @ModelAttribute("caseUpdateForm")
            FeedbackCaseUpdateForm form,
            BindingResult errors,
            @RequestParam Map<String, String> parameters,
            RedirectAttributes flash) {

        keepFilters(parameters, flash);

        if (errors.hasErrors()) {

            flash.addFlashAttribute(
                    "error",
                    validationMessage(errors)
            );

        } else {
            try {
                feedbackService.updateCase(id, form);

                flash.addFlashAttribute(
                        "success",
                        "Case status and priority saved."
                );

            } catch (FeedbackBusinessException ex) {
                flash.addFlashAttribute("error", ex.getMessage());
            }
        }

        return "redirect:/feedback/manage";
    }

    // Archive a completed case without deleting its history.
    @PreAuthorize("hasRole('LIBRARY_MANAGER')")
    @PostMapping("/feedback/manage/{id}/archive")
    public String archive(
            @PathVariable Long id,
            @RequestParam Map<String, String> parameters,
            RedirectAttributes flash) {

        keepFilters(parameters, flash);

        try {
            feedbackService.archive(id);

            flash.addFlashAttribute(
                    "success",
                    "Case archived. Its history is retained."
            );

        } catch (FeedbackBusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/feedback/manage";
    }

    // Supply the data required by feedback.html.
    private void populateMemberPage(Model model) {

        List<FeedbackItem> items =
                feedbackService.currentMemberItems();

        model.addAttribute("items", items);
        model.addAttribute("replies", feedbackService.repliesFor(items));
        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("categories", feedbackService.categories());
    }

    // Supply the data required by feedback-edit.html.
    private void populateEditPage(Model model, FeedbackItem item) {

        model.addAttribute("item", item);
        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("categories", feedbackService.categories());
    }

    // Convert validation failures into a user-friendly message.
    private String validationMessage(BindingResult errors) {

        // Avoid displaying internal Java enum-conversion details.
        if (errors.getFieldErrors()
                .stream()
                .anyMatch(error -> error.isBindingFailure())) {

            return "Please select valid values from the available options.";
        }

        return errors.getAllErrors()
                .get(0)
                .getDefaultMessage();
    }

    // Preserve an invalid reply so the user can correct and resend it.
    private void keepReply(
            RedirectAttributes flash,
            Long id,
            String message,
            String error) {

        flash.addFlashAttribute("error", error);
        flash.addFlashAttribute("retryCaseId", id);
        flash.addFlashAttribute(
                "retryMessage",
                message == null ? "" : message
        );
    }

    // Preserve recognised manager filters after a POST redirect.
    private void keepFilters(
            Map<String, String> parameters,
            RedirectAttributes flash) {

        keepEnumFilter(
                parameters.get("filterType"),
                "type",
                FeedbackType.values(),
                flash
        );

        keepEnumFilter(
                parameters.get("filterStatus"),
                "status",
                FeedbackStatus.values(),
                flash
        );

        keepEnumFilter(
                parameters.get("filterPriority"),
                "priority",
                FeedbackPriority.values(),
                flash
        );

        String keyword = parameters.get("filterKeyword");

        if (keyword != null && !keyword.isBlank()) {
            flash.addAttribute("keyword", keyword.trim());
        }
    }

    // Keep a filter only if it matches a valid enum value.
    private void keepEnumFilter(
            String value,
            String name,
            Enum<?>[] options,
            RedirectAttributes flash) {

        if (value != null
                && Arrays.stream(options)
                .anyMatch(option -> option.name().equals(value))) {

            flash.addAttribute(name, value);
        }
    }
}