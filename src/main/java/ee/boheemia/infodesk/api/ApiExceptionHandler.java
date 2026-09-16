package ee.boheemia.infodesk.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import com.openai.errors.OpenAIException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	public record ErrorResponse(String error, List<String> details) {
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse validation(MethodArgumentNotValidException e) {
		List<String> details = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.sorted()
				.toList();
		return new ErrorResponse("Invalid request", details);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse malformedBody(HttpMessageNotReadableException e) {
		return new ErrorResponse("Malformed request body", List.of("Request body must be valid JSON."));
	}

	@ExceptionHandler({ OpenAIException.class, RestClientException.class })
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public ErrorResponse upstreamFailure(RuntimeException e) {
		log.error("Upstream model call failed: {}", e.getClass().getName(), e);
		return new ErrorResponse("Upstream service unavailable", List.of("Please try again later."));
	}

}
