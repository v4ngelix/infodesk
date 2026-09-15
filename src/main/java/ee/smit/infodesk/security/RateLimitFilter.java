package ee.smit.infodesk.security;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final long WINDOW_MILLIS = 60_000;

	private final int requestsPerMinute;

	private final Map<String, Deque<Long>> requestsByClient = new ConcurrentHashMap<>();

	public RateLimitFilter(@Value("${infodesk.rate-limit.requests-per-minute:10}") int requestsPerMinute) {
		this.requestsPerMinute = requestsPerMinute;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/v1/agent/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (tryAcquire(request.getRemoteAddr(), System.currentTimeMillis())) {
			chain.doFilter(request, response);
			return;
		}
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"Too many requests\",\"details\":[\"Limit is "
				+ requestsPerMinute + " requests per minute.\"]}");
	}

	private boolean tryAcquire(String client, long now) {
		boolean[] allowed = new boolean[1];
		requestsByClient.compute(client, (key, timestamps) -> {
			Deque<Long> window = timestamps == null ? new ArrayDeque<>() : timestamps;
			while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MILLIS) {
				window.pollFirst();
			}
			allowed[0] = window.size() < requestsPerMinute;
			if (allowed[0]) {
				window.addLast(now);
			}
			return window.isEmpty() ? null : window;
		});
		return allowed[0];
	}

}
