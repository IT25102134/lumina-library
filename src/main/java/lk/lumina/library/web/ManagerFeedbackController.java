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
 * Handles feedback actions available to a library manager.
 */
@Controller
@RequestMapping("/feedback/manage")
@PreAuthorize("hasRole('LIBRARY_MANAGER')")
public class ManagerFeedbackController {

    /**
     * WITHDRAWN is excluded because only a member can withdraw a case.
     */
    private static final List<FeedbackStatus> MANAGER_STATUSES =
            List.of(
                    FeedbackStatus.OPEN,
                    FeedbackStatus.IN_REVIEW,
                    FeedbackStatus.RESOLVED,
                    FeedbackStatus.CLOSED
            );

    private final FeedbackService feedbackService;

    /**
     * Spring injects FeedbackService through the constructor.
     */
    public ManagerFeedbackController(
            FeedbackService feedbackService) {

        this.feedbackService = feedbackService;
    }

    /**
     * Displays the manager queue with optional search and filters.
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
         * Remove unnecessary spaces from the keyword.
         */
        String cleanKeyword =
                keyword == null
                        ? ""
                        : keyword.trim();

        /*
         * Search active cases using the selected filters.
         */
        List<FeedbackItem> items =
                feedbackService.searchForManager(
                        type,
                        status,
                        priority,
                        cleanKeyword
                );

        /*
         * Add all data required by feedback-manage.html.
         */
        addManagerPageData(
                model,
                items,
                type,
                status,
                priority,
                cleanKeyword
        );

        return "feedback-manage";
    }

    /**
     * Adds a manager reply and notifies the member.
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
         * Do not send an invalid reply to the service.
         */
        if (errors.hasErrors()) {

            flash.addFlashAttribute(
                    "error",
                    firstError(errors)
            );

            return managerRedirect();
        }

        try {

            /*
             * The service saves the reply, changes OPEN to IN_REVIEW
             * and notifies the member.
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

        return managerRedirect();
    }

    /**
     * Updates the workflow status and handling priority.
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
         * Status and priority are required.
         */
        if (errors.hasErrors()) {

            flash.addFlashAttribute(
                    "error",
                    firstError(errors)
            );

            return managerRedirect();
        }

        try {

            /*
             * The service validates the status transition
             * before changing the entity.
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

        return managerRedirect();
    }

    /**
     * Archives a completed case without deleting its history.
     *
     * URL: POST /feedback/manage/{id}/archive
     */
    @PostMapping("/{id}/archive")
    public String archiveCase(

            @PathVariable Long id,

            RedirectAttributes flash) {

        try {

            /*
             * Only CLOSED and WITHDRAWN cases can be archived.
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

        return managerRedirect();
    }

    /**
     * Adds all data required by feedback-manage.html.
     */
    private void addManagerPageData(

            Model model,

            List<FeedbackItem> items,

            FeedbackType selectedType,

            FeedbackStatus selectedStatus,

            FeedbackPriority selectedPriority,

            String keyword) {

        /*
         * Add the filtered feedback cases.
         */
        model.addAttribute(
                "items",
                items
        );

        /*
         * Add replies belonging to the displayed cases.
         */
        model.addAttribute(
                "replies",
                feedbackService.repliesFor(items)
        );

        /*
         * Add dashboard counts.
         */
        model.addAttribute(
                "stats",
                feedbackService.stats()
        );

        /*
         * Add values for the search filters.
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
         * Add only the statuses that a manager may select.
         */
        model.addAttribute(
                "managerStatuses",
                MANAGER_STATUSES
        );

        model.addAttribute(
                "priorities",
                FeedbackPriority.values()
        );

        /*
         * Keep selected filter values visible after searching.
         */
        model.addAttribute(
                "selectedType",
                selectedType
        );

        model.addAttribute(
                "selectedStatus",
                selectedStatus
        );

        model.addAttribute(
                "selectedPriority",
                selectedPriority
        );

        model.addAttribute(
                "keyword",
                keyword
        );
    }

    /**
     * Returns the first validation message.
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

    /**
     * Keeps every manager POST action on the manager queue page.
     */
    private String managerRedirect() {

        return "redirect:/feedback/manage";
    }
}