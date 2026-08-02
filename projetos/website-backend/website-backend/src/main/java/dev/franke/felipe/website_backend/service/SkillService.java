package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.SkillDTO;
import dev.franke.felipe.website_backend.dto.SkillLevel;
import dev.franke.felipe.website_backend.dto.SkillParaIA;
import dev.franke.felipe.website_backend.dto.SkillRequest;
import dev.franke.felipe.website_backend.exception.SkillException;
import dev.franke.felipe.website_backend.exception.SkillNotFoundException;
import dev.franke.felipe.website_backend.model.Skill;
import dev.franke.felipe.website_backend.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

    private final SkillRepository skillRepository;

    public List<SkillDTO> getSkills() {
        log.info("Entered method to retrieve all skills");
        List<Skill> skills = skillRepository.findAll();
        log.trace("Skills found: {}", skills);
        return skills
                .stream().map(
                skill -> new SkillDTO(
                        skill.getId(),
                        skill.getName(),
                        skill.getCategory(),
                        resolveSkillLevel(skill.getLevel())
                )
        ).toList();
    }

    /**
     * Habilidades no formato consumido pelas tool calls do chatbot: sem o UUID, que a IA
     * nunca usa e só consome tokens, e com o nível já em texto legível.
     */
    public List<SkillParaIA> getSkillsParaChatbot() {
        log.info("Montando a lista de habilidades para o chatbot");
        return skillRepository.findAll().stream()
                .map(skill -> new SkillParaIA(
                        skill.getName(),
                        skill.getCategory(),
                        resolveSkillLevel(skill.getLevel()).getDescription()
                ))
                .toList();
    }

    public SkillDTO getSkillById(String skillId) {
        log.info("Entered method to get skill with id: {}", skillId);
        UUID skillIdUUID = parseSkillId(skillId);
        Skill skill = skillRepository.findById(skillIdUUID).orElseThrow(
                () -> new SkillNotFoundException("Skill with id: " + skillId + " not found")
        );
        log.trace("Found skill: {}", skill);
        log.trace("Returning DTO to requester");
        return new SkillDTO(
                skill.getId(),
                skill.getName(),
                skill.getCategory(),
                resolveSkillLevel(skill.getLevel())
        );
    }

    public void saveSkill(SkillRequest skillRequest) {
        log.info("Entered method to save a new Skill. Request: {}", skillRequest);
        if (skillRequest == null) throw new SkillException("Request cannot be null");
        log.trace("Checking if the Skill already exists");
        if (skillRepository.existsByName(skillRequest.name())) {
            throw new SkillException("Skill with name " + skillRequest.name() + " already exists");
        }
        log.trace("Skill does not exist");
        Skill skill = new Skill();
        skill.setName(skillRequest.name());
        skill.setCategory(skillRequest.category());
        skill.setLevel((short) skillRequest.level());
        skillRepository.save(skill);
        log.trace("Skill has been saved successfully");
    }

    public void updateSkill(String skillId, SkillRequest skillRequest) {
        log.info("Entered method to update a new Skill. Request: {} and Skill ID: {}", skillRequest, skillId);
        if (skillRequest == null) throw new SkillException("Request cannot be null");
        if (skillId == null) throw new SkillException("Skill ID cannot be null");
        UUID skillIdUUID = parseSkillId(skillId);
        Skill existingSkill = skillRepository.findById(skillIdUUID).orElseThrow(
                () -> new SkillNotFoundException("Skill with id: " + skillId + " not found")
        );
        log.trace("Existing Skill found: {}", existingSkill);
        existingSkill.setName(skillRequest.name());
        existingSkill.setCategory(skillRequest.category());
        existingSkill.setLevel((short) skillRequest.level());
        skillRepository.save(existingSkill);
        log.trace("Skill has been updated successfully");
    }

    public void deleteSkill(String skillId) {
        log.info("Entered method to delete a skill with id: {}", skillId);
        UUID skillIdUUID = parseSkillId(skillId);
        Skill existingSkill = skillRepository.findById(skillIdUUID).orElseThrow(
                () -> new SkillNotFoundException("Skill with id: " + skillId + " not found")
        );
        log.trace("Existing Skill found to be deleted: {}", existingSkill);
        skillRepository.delete(existingSkill);
        log.trace("Skill has been deleted successfully");
    }

    private UUID parseSkillId(String skillId) {
        log.trace("Checking if valid ID was informed: {}", skillId);
        try {
            UUID parsed = UUID.fromString(skillId);
            log.trace("Valid ID: {}", skillId);
            return parsed;
        } catch (Exception parsingException) {
            log.warn("Unparsable ID! {}", skillId);
            throw new SkillException("Unparsable ID: " + skillId);
        }
    }

    private SkillLevel resolveSkillLevel(int level) {
        try {
            return SkillLevel.fromLevel(level);
        } catch (IllegalArgumentException invalidLevelException) {
            log.error("Skill has an invalid stored level: {}", level);
            throw new SkillException("Invalid stored skill level: " + level);
        }
    }
}
