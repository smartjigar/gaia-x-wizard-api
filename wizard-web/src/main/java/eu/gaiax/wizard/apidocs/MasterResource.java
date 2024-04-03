package eu.gaiax.wizard.apidocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class MasterResource {


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Create Tenant",
            description = "This endpoint create tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant registered successfully."),
            @ApiResponse(responseCode = "400", description = "Tenant already exits with alias."),
            @ApiResponse(responseCode = "400", description = "Tenant already exits with name.")
    })
    public @interface CreateTenantApiDocs {
    }

}
