package sandipchitale.helm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
									System.out.println("Namespace: " + secret.getMetadata().getNamespace() + " release " + matcher.group(1) + " revision: " + matcher.group(2));
									JsonNode jsonNode = mapper.readTree(releaseJsonString);
									System.out.println("Manifest:");
									System.out.println(jsonNode.get("manifest").asText().replace("\\n", "\n"));
//									String indentedReleaseJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
//									System.out.println(indentedReleaseJsonString);
//									System.out.println("------------------------------------------------------------------");
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
