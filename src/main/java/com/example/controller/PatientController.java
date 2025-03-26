package com.example.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.exception.PatientNotFoundException;
import com.example.model.Patient;
import com.example.repository.PatientRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientRepository patientRepository;

    public PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping
    public String listPatients(@RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            Model model) {
        Page<Patient> patientPage = patientRepository.findAll(PageRequest.of(page, size));
        model.addAttribute("patients", patientPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", patientPage.getTotalPages());
        return "patients/list";
    }

    @GetMapping("/{id}")
    public String viewPatient(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
            model.addAttribute("patient", patient);
            return "patients/details";
        } catch (PatientNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/patients";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Patient patient) {
        return "patients/create";
    }

    @PostMapping
    public String createPatient(@Valid Patient patient, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("patient", patient);
            return "patients/create";
        }

        try {
            // メール重複チェック
            if (patientRepository.existsByEmail(patient.getEmail())) {
                throw new DataIntegrityViolationException("");
            }

            patientRepository.save(patient);
            return "redirect:/patients";
        } catch (DataIntegrityViolationException e) {
            // データベースレベルの重複エラー
            result.rejectValue("email", "email.duplicate", "このメールアドレスは既に使用されています");
            return "patients/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
            model.addAttribute("patient", patient);
            return "patients/edit";
        } catch (PatientNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/patients";
        }
    }

    @PutMapping("/{id}")
    public String updatePatient(@PathVariable("id") Long id, @Valid Patient patient, BindingResult result, RedirectAttributes redirectAttributes) {
        try {
            // 既存の患者データ取得
            Patient existingPatient = patientRepository.findById(id)
            .orElseThrow(() -> new PatientNotFoundException(id));

            // メールアドレス変更時の重複チェック
            if (!existingPatient.getEmail().equals(patient.getEmail())) {
                if (patientRepository.existsByEmail(patient.getEmail())) {
                    throw new DataIntegrityViolationException("");
                }
            }

            if (result.hasErrors()) {
                return "patients/edit";
            }

            patient.setId(id);
            patientRepository.save(existingPatient);
            
            return "redirect:/patients/" + id;
        } catch (PatientNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/patients/" + id;
        } catch (DataIntegrityViolationException e) {
            // データベースレベルの重複エラー
            result.rejectValue("email", "email.duplicate", "このメールアドレスは既に使用されています");
            return "patients/edit";
        }
    }

    @DeleteMapping("/{id}")
    public String deletePatient(@PathVariable("id") Long id) {
        patientRepository.deleteById(id);
        return "redirect:/patients";
    }

}
