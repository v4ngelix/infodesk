package ee.smit.infodesk.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import ee.smit.infodesk.InfodeskApplication;
import ee.smit.infodesk.knowledgebase.KnowledgeBaseRepository;

/** The agent may only ever call these tools (task §5 allowlist). */
class ToolAllowlistTest {

	private static final Set<String> ALLOWED_TOOLS = Set.of("listTopics", "searchKnowledgeBase", "getDocument");

	@Test
	void knowledgeBaseToolsDeclaresExactlyTheAllowedToolMethods() {
		Set<String> toolMethods = Arrays.stream(KnowledgeBaseTools.class.getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(Tool.class))
				.map(method -> method.getName())
				.collect(Collectors.toSet());

		assertThat(toolMethods).isEqualTo(ALLOWED_TOOLS);
	}

	@Test
	void springAiExposesExactlyTheAllowedToolsWithDescriptions() {
		var callbacks = ToolCallbacks.from(new KnowledgeBaseTools(new KnowledgeBaseRepository()));

		assertThat(callbacks).extracting(callback -> callback.getToolDefinition().name())
				.containsExactlyInAnyOrderElementsOf(ALLOWED_TOOLS);
		assertThat(callbacks).allSatisfy(callback -> assertThat(callback.getToolDefinition().description()).isNotBlank());
	}

	@Test
	void noOtherClassInApplicationDeclaresTools() {
		var scanner = new ClassPathScanningCandidateComponentProvider(false) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				return true;
			}
		};
		scanner.addIncludeFilter((reader, factory) -> reader.getAnnotationMetadata().hasAnnotatedMethods(Tool.class.getName()));

		Set<String> classesWithTools = scanner.findCandidateComponents(InfodeskApplication.class.getPackageName()).stream()
				.map(BeanDefinition::getBeanClassName)
				.collect(Collectors.toSet());

		assertThat(classesWithTools).containsExactly(KnowledgeBaseTools.class.getName());
	}

}
