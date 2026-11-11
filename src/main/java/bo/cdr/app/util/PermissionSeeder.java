package bo.cdr.app.util;

import bo.cdr.app.appsecurity.domain.entity.Permission;
import bo.cdr.app.appsecurity.domain.service.interfaces.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionSeeder implements CommandLineRunner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final PermissionService permissionService;

    @Override
    public void run(String... args) {

        Map<RequestMappingInfo, HandlerMethod> map =
                handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {

            RequestMappingInfo info = entry.getKey();

            Set<String> urls = info.getPatternValues();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

            for(String url : urls){
                for(RequestMethod method : methods){

                    String normalizedUrl = normalizeUrl(url);

                    String code = generateCode(method.name(), normalizedUrl);

                    if(!permissionService.existsByCode(code)){

                        Permission permission = new Permission();

                        permission.setName(buildName(method.name(), normalizedUrl));

                        permission.setDescription(
                                "Permiso para " + method.name() + " " + url
                        );

                        permission.setHttpMethod(method.name());

                        permission.setUrl(url);

                        permission.setUrlWithoutIds(normalizedUrl);

                        permission.setCode(code);

                        permissionService.create(permission);
                    }
                }
            }
        }
    }

    private String normalizeUrl(String url){
        return url.replaceAll("/\\{[^/]+\\}", "");
    }

    private String generateCode(String method, String url){
        return method + "_" +
                url.replace("/", "_")
                        .replace("{","")
                        .replace("}","")
                        .toUpperCase();
    }

    private String buildName(String method, String url){
        return method + " " + url;
    }
}