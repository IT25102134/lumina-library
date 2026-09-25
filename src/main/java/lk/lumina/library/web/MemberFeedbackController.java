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

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the library member's feedback pages and actions.
 */
@Controller
@RequestMapping("/feedback")
@PreAuthorize("hasRole('MEMBER')")
public class MemberFeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Spring injects the FeedbackService through the constructor.
     */
    public MemberFeedbackController(
            FeedbackService feedbackService) {

        this.feedbackService = feedbackService;
    }

    /**
     * Displays the feedback form and the logged-in member's cases.
     *
     * URL: GET /feedback
     */
    @GetMapping
    public String showFeedbackPage(Model model) {

        /*
         * Create an empty form when the page is opened
         * for the first time.
         */
        if (!model.containsAttribute("feedbackForm")) {

            model.addAttribute(
                    "feedbackForm",
                    new FeedbackForm()
            );
        }

        /*
         * Add cases, replies, types and categories.
         */
        addMemberPageData(model);

        return "feedback";
    }

    /**
     * Submits a new feedback, complaint or suggestion.
     *
     * URL: POST /feedback
     */
    @PostMapping
    public String submitCase(

            @Valid
            @ModelAttribute("feedbackForm")
            FeedbackForm form,

            BindingResult errors,

            Model model,

            RedirectAttributes flash) {

        /*
         * Do not save the form when validation fails.
         */
        if (errors.hasErrors()) {

            addMemberPageData(model);

            return "feedback";
        }

        /*
         * Send the validated form to the service layer.
         */
        FeedbackItem item =
                feedbackService.submit(form);

        /*
         * Display the generated reference number.
         */
        flash.addFlashAttribute(
                "success",
                "Your message was submitted. Reference: "
                        + item.getReferenceNumber()
        );

        /*
         * Redirecting prevents duplicate form submissions.
         */
        return "redirect:/feedback";
    }

    /**
     * Displays the edit page for an editable case.
     *
     * URL: GET /feedback/{id}/edit
     */
    @GetMapping("/{id}/edit")
    public String showEditPage(

            @PathVariable Long id,

            Model model,

            RedirectAttributes flash) {

        /*
         * The service checks whether the case belongs
         * to the logged-in member.
         */
        FeedbackItem item =
                feedbackService.currentMemberItem(id);

        try {

            /*
             * The case must be OPEN and must not contain replies.
             */
            feedbackService.assertMemberCanEdit(item);

        } catch (FeedbackBusinessException exception) {

            flash.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );

            return "redirect:/feedback";
        }

        /*
         * Copy existing case values to the form object.
         */
        FeedbackForm form =
                FeedbackForm.from(item);

        addEditPageData(
                model,
                item,
                form
        );

        return "feedback-edit";
    }

    /**
     * Saves changes to an existing feedback case.
     *
     * URL: POST /feedback/{id}/edit
     */
    @PostMapping("/{id}/edit")
    public String updateCase(

            @PathVariable Long id,

            @Valid
            @ModelAttribute("feedbackForm")
            FeedbackForm form,

            BindingResult errors,

            Model model,

            RedirectAttributes flash) {

        /*
         * Verify that the case belongs to the member.
         */
        FeedbackItem item =
                feedbackService.currentMemberItem(id);

        /*
         * Return to the edit page when validation fails.
         */
        if (errors.hasErrors()) {

            addEditPageData(
                    model,
                    item,
                    form
            );

            return "feedback-edit";
        }

        try {

            /*
             * The service checks the edit business rules.
             */
            feedbackService.updateByMember(
                    id,
                    form
            );

            flash.addFlashAttribute(
                    "success",
                    "Feedback case updated successfully."
            );

        } catch (FeedbackBusinessException exception) {

            flash.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/feedback";
    }

    /**
     * Withdraws an OPEN or IN_REVIEW case.
     *
     * URL: POST /feedback/{id}/withdraw
     */
    @PostMapping("/{id}/withdraw")
    public String withdrawCase(

            @PathVariable Long id,

            RedirectAttributes flash) {

        try {

            /*
             * The service checks ownership and case status.
             */
            feedbackService.withdrawByMember(id);

            flash.addFlashAttribute(
                    "success",
                    "Feedback case withdrawn."
            );

        } catch (FeedbackBusinessException exception) {

            flash.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/feedback";
    }

    /**
     * Adds a member reply to an active case.
     *
     * URL: POST /feedback/{id}/reply
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
         * Redirect with the first validation error.
         */
        if (errors.hasErrors()) {

            flash.addFlashAttribute(
                    "error",
                    firstError(errors)
            );

            return "redirect:/feedback";
        }

        try {

            /*
             * The service checks ownership, case status
             * and reply contents.
             */
            feedbackService.replyByMember(
                    id,
                    replyForm.getMessage()
            );

            flash.addFlashAttribute(
                    "success",
                    "Your reply was added to the case."
            );

        } catch (FeedbackBusinessException exception) {

            flash.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/feedback";
    }

    /**
     * Adds the data required by feedback.html.
     */
    private void addMemberPageData(Model model) {

        List<FeedbackItem> items =
                feedbackService.currentMemberItems();

        model.addAttribute(
                "items",
                items
        );

        model.addAttribute(
                "replies",
                feedbackService.repliesFor(items)
        );

        model.addAttribute(
                "types",
                FeedbackType.values()
        );

        model.addAttribute(
                "categories",
                feedbackService.categories()
        );
    }

    /**
     * Adds the data required by feedback-edit.html.
     */
    private void addEditPageData(

            Model model,

            FeedbackItem item,

            FeedbackForm form) {

        model.addAttribute(
                "item",
                item
        );

        model.addAttribute(
                "feedbackForm",
                form
        );

        model.addAttribute(
                "types",
                FeedbackType.values()
        );

        model.addAttribute(
                "categories",
                feedbackService.categories()
        );
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
