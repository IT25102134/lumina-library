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

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the library manager's feedback pages and actions.
 */
@Controller
@RequestMapping("/feedback/manage")
@PreAuthorize("hasRole('LIBRARY_MANAGER')")
public class ManagerFeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Spring injects the FeedbackService through the constructor.
     */
    public ManagerFeedbackController(
            FeedbackService feedbackService) {

        this.feedbackService = feedbackService;
    }

    /**
     * Displays the manager feedback page.
     *
     * The manager can filter cases by type, status,
     * priority and keyword.
     *
     * URL: GET /feedback/manage
     */
    @GetMapping
    public String showManagerPage(

            @RequestParam(required = false)
            FeedbackType type,

            @RequestParam(required = false)
            FeedbackStatus status,

            @RequestParam(required = false)
            FeedbackPriority priority,

            @RequestParam(required = false)
            String keyword,

            Model model) {

        /*
         * Search cases using the selected filters.
         */
        List<FeedbackItem> items =
                feedbackService.searchForManager(
                        type,
                        status,
                        priority,
                        keyword
                );

        /*
         * Add filtered cases to the page.
         */
        model.addAttribute(
                "items",
                items
        );

        /*
         * Add conversation replies for the displayed cases.
         */
        model.addAttribute(
                "replies",
                feedbackService.repliesFor(items)
        );

        /*
         * Add manager dashboard statistics.
         */
        model.addAttribute(
                "stats",
                feedbackService.stats()
        );

        /*
         * Add values required by filter dropdowns.
         */
        model.addAttribute(
                "types",
                FeedbackType.values()
        );

        model.addAttribute(
                "statuses",
                FeedbackStatus.values()
        );

        /*
         * WITHDRAWN is excluded because only members
         * can withdraw their cases.
         */
        model.addAttribute(
                "managerStatuses",
                List.of(
                        FeedbackStatus.OPEN,
                        FeedbackStatus.IN_REVIEW,
                        FeedbackStatus.RESOLVED,
                        FeedbackStatus.CLOSED
                )
        );

        model.addAttribute(
                "priorities",
                FeedbackPriority.values()
        );

        /*
         * Preserve the selected filter values.
         */
        model.addAttribute(
                "selectedType",
                type
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        model.addAttribute(
                "selectedPriority",
                priority
        );

        model.addAttribute(
                "keyword",
                keyword == null
                        ? ""
                        : keyword
        );

        return "feedback-manage";
    }

    /**
     * Adds a manager reply to a feedback case.
     *
     * URL: POST /feedback/manage/{id}/reply
     */
    @PostMapping("/{id}/reply")
    public String replyToCase(

            @PathVariable Long id,

            @Valid
            @ModelAttribute("replyForm")
            FeedbackReplyForm replyForm,

            BindingResult errors,

            RedirectAttributes flash) {

        /*
         * Reject invalid reply messages.
         */
        if (errors.hasErrors()) {

            flash.addFlashAttribute(
                    "error",
                    firstError(errors)
            );

            return "redirect:/feedback/manage";
        }

        try {

            /*
             * Save the manager reply and notify the member.
             */
            feedbackService.replyByManager(
                    id,
                    replyForm.getMessage()
            );

            flash.addFlashAttribute(
                    "success",
                    "Reply sent and the member was notified."
            );

        } catch (FeedbackBusinessException exception) {

            flash.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/feedback/manage";
    }

    /**
     * Updates the case status and priority.
     *
     * URL: POST /feedback/manage/{id}/status
     */
    @PostMapping("/{id}/status")
    public String updateCase(

            @PathVariable Long id,

            @Valid
            @ModelAttribute("caseUpdateForm")
            FeedbackCaseUpdateForm caseUpdateForm,

            BindingResult errors,

            RedirectAttributes flash) {

        /*
         * Reject missing status or priority values.
         */
        if (errors.hasErrors()) {

            flash.addFlashAttribute(
                    "error",
                    firstError(errors)
            );

            return "redirect:/feedback/manage";
        }

        try {

            /*
             * The service checks whether the requested
             * status transition is allowed.
             */
            feedbackService.updateCase(
                    id,
                    caseUpdateForm
            );

            flash.addFlashAttribute(
                    "success",
                    "Case status and priority updated."
            );

        } catch (FeedbackBusinessException exception) {

            flash.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/feedback/manage";
    }

    /**
     * Archives a CLOSED or WITHDRAWN feedback case.
     *
     * URL: POST /feedback/manage/{id}/archive
     */
    @PostMapping("/{id}/archive")
    public String archiveCase(

            @PathVariable Long id,

            RedirectAttributes flash) {

        try {

            /*
             * Archiving hides the case without deleting its history.
             */
            feedbackService.archive(id);

            flash.addFlashAttribute(
                    "success",
                    "Case archived."
            );

        } catch (FeedbackBusinessException exception) {

            flash.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/feedback/manage";
    }

    /**
     * Returns the first validation error message.
     */
    private String firstError(
            BindingResult errors) {

        if (errors.getAllErrors().isEmpty()) {

            return "Please check the submitted values.";
        }

        return errors
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
    }
}
