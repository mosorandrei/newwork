package com.newwork.core.web;

import com.newwork.core.domain.Employee;
import com.newwork.core.service.EmployeeService;
import com.newwork.core.web.dto.EmployeeDtos.CreateEmployeeReq;
import com.newwork.core.web.dto.EmployeeDtos.UpdateEmployeeReq;
import com.newwork.core.web.support.Etags;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final Etags etags;

    public EmployeeController(EmployeeService employeeService, Etags etags) {
        this.employeeService = employeeService;
        this.etags = etags;
    }

    @GetMapping
    public List<Employee> all() { return employeeService.getAllEmployees(); }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> one(@PathVariable UUID id) {
        var e = employeeService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .eTag(etags.toEtag(e.getVersion()))
                .body(e);
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody @Valid CreateEmployeeReq body) {
        var e = new Employee();
        e.setFirstName(body.firstName().trim());
        e.setLastName(body.lastName().trim());
        var saved = employeeService.save(e);

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location)
                .eTag(etags.toEtag(saved.getVersion()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable UUID id,
                                           @RequestBody @Valid UpdateEmployeeReq body,
                                           @RequestHeader(value = "If-Match", required = false) String ifMatch) {

        var e = employeeService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        etags.assertMatches(e.getVersion(), ifMatch); // throws 428/412/409 as needed

        if (body.firstName() != null) e.setFirstName(body.firstName().trim());
        if (body.lastName()  != null) e.setLastName(body.lastName().trim());

        var saved = employeeService.save(e);
        return ResponseEntity.ok()
                .eTag(etags.toEtag(saved.getVersion()))
                .body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        var e = employeeService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        etags.assertMatches(e.getVersion(), ifMatch);

        employeeService.delete(e);
        return ResponseEntity.noContent().build();
    }
}
