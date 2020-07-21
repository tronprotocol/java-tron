package org.tron.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import java.util.Optional;

@Controller
@RequestMapping("${openapi.rosetta.base-path:}")
public class BlockApiController implements BlockApi {

    private final NativeWebRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public BlockApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

}
