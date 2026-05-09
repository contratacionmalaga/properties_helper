package local.jarios.properties.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import local.jarios.properties.common.util.Constantes;
import local.jarios.properties.exception.PropertiesManagerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesManagerServiceImplTest {

  private final PropertiesManagerService manager = PropertiesManagerServiceImpl.getInstance();

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    manager.setSensitiveKeys(null);
    manager.setConfigDir(tempDir.toString());
    manager.loadAllProperties();
    System.clearProperty("external.only");
  }

  @AfterEach
  void tearDown() {
    System.clearProperty("external.only");
  }

  @Test
  void loadAllPropertiesLoadsPropertiesFilesUsingNameWithoutExtension() throws IOException {
    writeProperties("app.properties", "app.name=properties-helper\napp.version=5.2.0\n");
    writeProperties("email.properties", "mail.password=secret\n");

    manager.loadAllProperties();

    assertThat(manager.getListFiles()).containsExactlyInAnyOrder("app", "email");
    assertThat(manager.getProperty("app", "app.name")).isEqualTo("properties-helper");
    assertThat(manager.getProperty("app.properties", "app.version")).isEqualTo("5.2.0");
  }

  @Test
  void loadAllPropertiesWithEmptyDirectoryClearsPreviousState() throws IOException {
    writeProperties("app.properties", "app.name=properties-helper\n");
    manager.loadAllProperties();

    Files.delete(tempDir.resolve("app.properties"));
    manager.loadAllProperties();

    assertThat(manager.getListFiles()).isEmpty();
    assertThat(manager.hasLoaded("app")).isFalse();
  }

  @Test
  void loadAllPropertiesRejectsMissingDirectory() {
    manager.setConfigDir(tempDir.resolve("missing").toString());

    assertThatThrownBy(manager::loadAllProperties)
        .isInstanceOf(PropertiesManagerException.class)
        .hasMessageContaining("Directorio inválido");
  }

  @Test
  void getPropertyThrowsWhenKeyDoesNotExistAnywhere() throws IOException {
    writeProperties("app.properties", "app.name=properties-helper\n");
    manager.loadAllProperties();

    assertThatThrownBy(() -> manager.getProperty("app", "missing.key"))
        .isInstanceOf(PropertiesManagerException.class)
        .hasMessageContaining("Clave inexistente");
  }

  @Test
  void getPropertyFallsBackToSystemPropertyWhenFileDoesNotContainKey() throws IOException {
    writeProperties("app.properties", "app.name=properties-helper\n");
    manager.loadAllProperties();
    System.setProperty("external.only", "from-system");

    assertThat(manager.getProperty("app", "external.only")).isEqualTo("from-system");
  }

  @Test
  void filePropertyHasPriorityOverSystemProperty() throws IOException {
    writeProperties("app.properties", "external.only=from-file\n");
    manager.loadAllProperties();
    System.setProperty("external.only", "from-system");

    assertThat(manager.getProperty("app", "external.only")).isEqualTo("from-file");
  }

  @Test
  void addPropertiesStoresDefensiveCopy() {
    Properties props = new Properties();
    props.setProperty("custom.key", "initial");

    manager.addProperties("custom.properties", props);
    props.setProperty("custom.key", "mutated");

    assertThat(manager.getProperty("custom", "custom.key")).isEqualTo("initial");
    assertThat(manager.hasLoaded("custom.properties")).isTrue();
  }

  @Test
  void getPropertiesReturnsDefensiveCopy() {
    Properties props = new Properties();
    props.setProperty("custom.key", "initial");
    manager.addProperties("custom", props);

    Properties returned = manager.getProperties("custom");
    returned.setProperty("custom.key", "mutated");

    assertThat(manager.getProperty("custom", "custom.key")).isEqualTo("initial");
  }

  @Test
  void getAllPropertiesReturnsUnmodifiableMapWithDefensiveValues() {
    Properties props = new Properties();
    props.setProperty("custom.key", "initial");
    manager.addProperties("custom", props);

    Map<String, Properties> allProperties = manager.getAllProperties();
    allProperties.get("custom").setProperty("custom.key", "mutated");

    assertThatThrownBy(() -> allProperties.put("other", new Properties()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(manager.getProperty("custom", "custom.key")).isEqualTo("initial");
  }

  @Test
  void setPropertyNormalizesFileName() {
    manager.setProperty("app.properties", "app.name", "properties-helper");

    assertThat(manager.hasLoaded("app")).isTrue();
    assertThat(manager.getProperty("app", "app.name")).isEqualTo("properties-helper");
  }

  @Test
  void validateRequiredKeysReturnsExpectedResult() {
    Properties props = new Properties();
    props.setProperty("app.name", "properties-helper");
    manager.addProperties("app", props);

    assertThat(manager.validateRequiredKeys("app.properties", Set.of("app.name"))).isTrue();
    assertThat(manager.validateRequiredKeys("app", Set.of("missing.key"))).isFalse();
    assertThat(manager.validateRequiredKeys("missing", Set.of("app.name"))).isFalse();
  }

  @Test
  void exportPropertiesToJsonMasksDefaultSensitiveKeys() {
    Properties props = new Properties();
    props.setProperty("mail.user", "user");
    props.setProperty("mail.password", "secret");
    manager.addProperties("email", props);

    String json = manager.exportPropertiesToJson("email.properties", true);

    assertThat(json).contains("\"mail.password\" : \"" + Constantes.KEY_SENSITIVE_VALUE + "\"");
    assertThat(json).doesNotContain("secret");
  }

  @Test
  void exportAllPropertiesToJsonMasksSensitiveKeys() {
    Properties props = new Properties();
    props.setProperty("api.token", "token-value");
    manager.addProperties("app", props);

    String json = manager.exportAllPropertiesToJson(true);

    assertThat(json).contains(Constantes.KEY_SENSITIVE_VALUE);
    assertThat(json).doesNotContain("token-value");
  }

  private void writeProperties(String fileName, String content) throws IOException {
    Files.writeString(tempDir.resolve(fileName), content);
  }
}
