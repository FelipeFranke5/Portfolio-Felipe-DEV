import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Skill, SkillRequest, SkillsService, resolveSkillLevel } from './skills.service';
import { environment } from '../../../environments/environment';

describe('SkillsService', () => {
  let service: SkillsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(SkillsService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET the skill list from /skills', () => {
    const skills: Skill[] = [
      {
        id: '22222222-2222-2222-2222-222222222222',
        name: 'Spring Boot',
        category: 'Back-end',
        skillLevel: 'WORK_EXPERIENCE',
      },
    ];

    let actualResponse: Skill[] | undefined;

    service.getSkills().subscribe((response) => {
      actualResponse = response;
    });

    const request = httpTestingController.expectOne(`${environment.apiUrl}/skills`);

    expect(request.request.method).toBe('GET');

    request.flush(skills);

    expect(actualResponse).toEqual(skills);
  });

  it('should GET a single skill by id from /skills/{id}', () => {
    const skill: Skill = {
      id: '22222222-2222-2222-2222-222222222222',
      name: 'Spring Boot',
      category: 'Back-end',
      skillLevel: 'WORK_EXPERIENCE',
    };

    let actualResponse: Skill | undefined;

    service.getSkillById(skill.id).subscribe((response) => {
      actualResponse = response;
    });

    const request = httpTestingController.expectOne(`${environment.apiUrl}/skills/${skill.id}`);

    expect(request.request.method).toBe('GET');

    request.flush(skill);

    expect(actualResponse).toEqual(skill);
  });

  it('should POST the payload to /skills when creating a skill', () => {
    const skillRequest: SkillRequest = { name: 'Docker', category: 'DevOps', level: 4 };

    service.createSkill(skillRequest).subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/skills`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(skillRequest);

    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should PUT the payload to /skills/{id} when updating a skill', () => {
    const skillId = '22222222-2222-2222-2222-222222222222';
    const skillRequest: SkillRequest = { name: 'Docker', category: 'DevOps', level: 5 };

    service.updateSkill(skillId, skillRequest).subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/skills/${skillId}`);

    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(skillRequest);

    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should DELETE /skills/{id} when removing a skill', () => {
    const skillId = '22222222-2222-2222-2222-222222222222';

    service.deleteSkill(skillId).subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/skills/${skillId}`);

    expect(request.request.method).toBe('DELETE');

    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});

describe('resolveSkillLevel', () => {
  // Este é o formato que a API realmente devolve hoje: o Jackson serializa o
  // enum pelo nome da constante, não pela descrição.
  it('should resolve the level when given the enum constant name', () => {
    const meta = resolveSkillLevel('WORK_EXPERIENCE');

    expect(meta?.level).toBe(5);
    expect(meta?.label).toBe('Avançado — com experiência prática');
    expect(meta?.tier).toBe(4);
  });

  it('should resolve the level when given the enum description', () => {
    const meta = resolveSkillLevel('Has intermediate knowledge about the topic');

    expect(meta?.level).toBe(3);
    expect(meta?.enumName).toBe('INTERMEDIATE_KNOWLEDGE');
  });

  it('should resolve the level when given the numeric level', () => {
    expect(resolveSkillLevel(1)?.enumName).toBe('ZERO_EXPERIENCE_STILL_LEARNING');
    expect(resolveSkillLevel(4)?.enumName).toBe('ADVANCED_KNOWLEDGE');
  });

  it('should return null when the value does not match any known level', () => {
    expect(resolveSkillLevel('NOT_A_LEVEL')).toBeNull();
    expect(resolveSkillLevel(9)).toBeNull();
    expect(resolveSkillLevel(null)).toBeNull();
    expect(resolveSkillLevel(undefined)).toBeNull();
  });
});
