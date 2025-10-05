package com.newwork.core.config;

import com.newwork.core.domain.*;
import com.newwork.core.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.newwork.core.security.Role.COWORKER;
import static com.newwork.core.security.Role.EMPLOYEE;
import static com.newwork.core.security.Role.MANAGER;
import static org.springframework.security.crypto.bcrypt.BCrypt.gensalt;
import static org.springframework.security.crypto.bcrypt.BCrypt.hashpw;

@Component
public class DataSeederRunner implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final FeedbackRepository feedbackRepository;
    private final AbsenceRequestRepository absenceRepo;


    public DataSeederRunner(EmployeeRepository employeeRepository,
                            UserRepository userRepository,
                            EmployeeProfileRepository profileRepository,
                            FeedbackRepository feedbackRepository,
                            AbsenceRequestRepository absenceRepo) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.feedbackRepository = feedbackRepository;
        this.absenceRepo = absenceRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var list = employeeRepository.findAll();

        var alice = list.stream().findFirst().orElseGet(() -> {
            var e = new Employee();
            e.setFirstName("Alice");
            e.setLastName("Ng");
            return employeeRepository.save(e);
        });

        Employee bob;
        if (list.size() > 1) {
            bob = list.get(1);
        } else {
            var e = new Employee();
            e.setFirstName("Bob");
            e.setLastName("Ionescu");
            bob = employeeRepository.save(e);
        }

        profileRepository.findByEmployeeId(alice.getId()).orElseGet(() -> {
            var p = new EmployeeProfile();
            p.setEmployee(alice); // managed in this tx
            p.setBio("Engineering manager at NEWWORK");
            p.setSkillsJson("{\"skills\":[\"Leadership\",\"Product\",\"Architecture\"]}");
            p.setSalary(new BigDecimal("150000"));
            p.setSsn("111223333");
            p.setAddress("Str. Exemplu 10, Bucharest");
            p.setContactEmail("alice@newwork.test");
            return profileRepository.save(p);
        });

        profileRepository.findByEmployeeId(bob.getId()).orElseGet(() -> {
            var p = new EmployeeProfile();
            p.setEmployee(bob); // managed in this tx
            p.setBio("Senior full-stack engineer");
            p.setSkillsJson("{\"skills\":[\"Java\",\"Spring\",\"React\"]}");
            p.setSalary(new BigDecimal("120000"));
            p.setSsn("999887777");
            p.setAddress("Str. Exemplu 20, Bucharest");
            p.setContactEmail("bob@newwork.test");
            return profileRepository.save(p);
        });

        if (userRepository.findByEmail("manager@newwork.test").isEmpty()) {
            var u = new User();
            u.setEmail("manager@newwork.test");
            u.setPasswordHash(hashpw("Passw0rd!", gensalt()));
            u.setRole(MANAGER);
            u.setEmployeeId(alice.getId());
            userRepository.save(u);
        }

        if (userRepository.findByEmail("bob@newwork.test").isEmpty()) {
            var u = new User();
            u.setEmail("bob@newwork.test");
            u.setPasswordHash(hashpw("Passw0rd!", gensalt()));
            u.setRole(EMPLOYEE);
            u.setEmployeeId(bob.getId());
            userRepository.save(u);
        }

        var carol = employeeRepository.findAll().stream()
                .filter(e -> "Carol".equals(e.getFirstName()))
                .findFirst()
                .orElseGet(() -> {
                    var e = new Employee();
                    e.setFirstName("Carol");
                    e.setLastName("Matei");
                    return employeeRepository.save(e);
                });

        profileRepository.findByEmployeeId(carol.getId()).orElseGet(() -> {
            var p = new EmployeeProfile();
            p.setEmployee(carol); // managed in this tx
            p.setBio("QA analyst");
            p.setSkillsJson("{\"skills\":[\"Testing\",\"Automation\"]}");
            p.setSalary(new BigDecimal("70000"));
            p.setSsn("123450000");
            p.setAddress("Str. Exemplu 30, Bucharest");
            p.setContactEmail("carol@newwork.test");
            return profileRepository.save(p);
        });

        if (userRepository.findByEmail("carol@newwork.test").isEmpty()) {
            var u = new User();
            u.setEmail("carol@newwork.test");
            u.setPasswordHash(hashpw("Passw0rd!", gensalt()));
            u.setRole(COWORKER);
            u.setEmployeeId(carol.getId());
            userRepository.save(u);
        }

        if (feedbackRepository.findByEmployeeIdOrderByCreatedAtDesc(bob.getId()).isEmpty()) {
            var f1 = new Feedback();
            f1.setEmployee(bob);
            f1.setAuthorEmployeeId(carol.getId());   // Carol wrote it
            f1.setTextOriginal("Pleasure to collaborate with Bob, he deliver fast.");
            f1.setTextPolished("It’s a pleasure to collaborate with Bob—he delivers fast.");
            f1.setPolishModel("seed");
            feedbackRepository.save(f1);
        }

        if (feedbackRepository.findByEmployeeIdOrderByCreatedAtDesc(alice.getId()).isEmpty()) {
            var f2 = new Feedback();
            f2.setEmployee(alice);
            f2.setAuthorEmployeeId(carol.getId());
            f2.setTextOriginal("Alice provide clear direction to the team.");
            f2.setTextPolished("Alice provides clear direction to the team.");
            f2.setPolishModel("seed");
            feedbackRepository.save(f2);
        }

        if (absenceRepo.findByEmployeeIdOrderByStartDateDesc(bob.getId())
                .stream().noneMatch(a -> a.getStatus() == AbsenceStatus.PENDING)) {
            var a = new AbsenceRequest();
            a.setEmployee(bob);
            a.setType(AbsenceType.VACATION);
            a.setStartDate(LocalDate.now().plusDays(7));
            a.setEndDate(LocalDate.now().plusDays(12));
            a.setReason("Family trip");
            a.setStatus(AbsenceStatus.PENDING);
            absenceRepo.save(a);
        }

        if (absenceRepo.findByEmployeeIdOrderByStartDateDesc(alice.getId())
                .stream().noneMatch(x -> x.getStatus() == AbsenceStatus.APPROVED)) {
            var a2 = new AbsenceRequest();
            a2.setEmployee(alice);
            a2.setType(AbsenceType.SICK);
            a2.setStartDate(LocalDate.now().minusDays(30));
            a2.setEndDate(LocalDate.now().minusDays(28));
            a2.setReason("Flu");
            a2.setStatus(AbsenceStatus.APPROVED);
            a2.setManagerComment("Approved retrospectively");
            absenceRepo.save(a2);
        }
    }
}
