package cl.pedidos360.notify.dlq;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dlq")
public class DlqController {

    private final DlqService service;

    public DlqController(DlqService service) {
        this.service = service;
    }

    @PostMapping("/{dominio}/inspeccionar")
    public Map<String, Object> inspeccionar(
            @PathVariable DlqService.Dominio dominio) {

        return service.inspeccionar(dominio);
    }
}