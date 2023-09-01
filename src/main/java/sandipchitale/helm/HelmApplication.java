package sandipchitale.helm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.javatuples.Pair;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@SpringBootApplication
public class HelmApplication {

	@Bean
	public CommandLineRunner clr () {
	    return (args) -> {
			try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build();) {
				Pattern helmSecretNamePattern = Pattern.compile("^\\Qsh.helm.release.v1.\\E([^.]+)\\Q.v\\E(\\d+)");
				ObjectMapper mapper = new ObjectMapper();

				kubernetesClient
						.secrets()
						.inAnyNamespace()
						.list()
						.getItems()
						.stream()
						.map((Secret secret) -> {
                            return new Pair<Secret, String>(secret, secret.getMetadata().getName());
                        })
						.collect(Collectors.toSet())
						.stream()
						.forEach(pair -> {
							Matcher matcher = helmSecretNamePattern.matcher(pair.getValue1());
							if (matcher.matches()) {

								Secret secret = pair.getValue0();
								String release = secret.getData().get("release");
								byte[] decodedRelease = Base64Coder.decode(release);
								decodedRelease = Base64Coder.decode(new String(decodedRelease, StandardCharsets.UTF_8));
								try {
									GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(decodedRelease));
									ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
									StreamUtils.copy(gzipInputStream, byteArrayOutputStream);
									String releaseJsonString = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
									System.out.println("------------------------------------------------------------------");
									System.out.println("Namespace: " + secret.getMetadata().getNamespace() + " release " + matcher.group(1) + " revision: " + matcher.group(2));
									JsonNode jsonNode = mapper.readTree(releaseJsonString);
									System.out.println("--------------------------------");

									System.out.println("Chart: " + jsonNode.get("name").asText());
									System.out.println("Status: " + jsonNode.get("info").get("status").asText());
									System.out.println("--------------------------------");

									JsonNode valuesNode = jsonNode.get("chart").get("values");
									System.out.println("Values:");
									System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(valuesNode));
									System.out.println("--------------------------------");

									System.out.println("Templates:");
									ArrayNode templates = (ArrayNode) jsonNode.get("chart").get("templates");
									templates.forEach(template -> {
										System.out.println("Template: " + template.get("name").asText());

										System.out.println(new String(Base64Coder.decode(template.get("data").asText()), StandardCharsets.UTF_8));
										System.out.println("----");
									});
									System.out.println("--------------------------------");

									System.out.println("Manifest:");
									System.out.println(jsonNode.get("manifest").asText().replace("\\n", "\n"));
									System.out.println("--------------------------------");

									System.out.println("Hooks:");
									ArrayNode hooks = (ArrayNode) jsonNode.get("hooks");
									hooks.forEach(hook -> {
										System.out.println("Hook: " + hook.get("path").asText() + " Events: " + hook.get("events"));
										System.out.println(hook.get("manifest").asText().replace("\\n", "\n"));
										System.out.println("----");
									});
									System.out.println("--------------------------------");

									System.out.println("Notes: ");
									System.out.println(jsonNode.get("info").get("notes").asText().replace("\\n", "\n"));
									System.out.println("------------------------------------------------------------------");
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							}
						});
			}
	    };
	}

	public static void main(String[] args) {
		SpringApplication.run(HelmApplication.class, args);
	}

}
