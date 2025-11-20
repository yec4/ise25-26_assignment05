package de.seuhd.campuscoffee.acctest;

import de.seuhd.campuscoffee.api.dtos.PosDto;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.PosService;
import io.cucumber.java.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static de.seuhd.campuscoffee.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the POS Cucumber tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
public class CucumberPosSteps {
    static final PostgreSQLContainer<?> postgresContainer;

    static {
        // share the same testcontainers instance across all Cucumber tests
        postgresContainer = getPostgresContainer();
        postgresContainer.start();
        // testcontainers are automatically stopped when the JVM exits
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        configurePostgresContainers(registry, postgresContainer);
    }

    @Autowired
    protected PosService posService;

    @LocalServerPort
    private Integer port;

    @Before
    public void beforeEach() {
        posService.clear();
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @After
    public void afterEach() {
        posService.clear();
    }

    private List<PosDto> createdPosList;
    private PosDto updatedPos;

    /**
     * Register a Cucumber DataTable type for PosDto.
     * @param row the DataTable row to map to a PosDto object
     * @return the mapped PosDto object
     */
    @DataTableType
    @SuppressWarnings("unused")
    public PosDto toPosDto(Map<String,String> row) {
        return PosDto.builder()
                .name(row.get("name"))
                .description(row.get("description"))
                .type(PosType.valueOf(row.get("type")))
                .campus(CampusType.valueOf(row.get("campus")))
                .street(row.get("street"))
                .houseNumber(row.get("houseNumber"))
                .postalCode(Integer.parseInt(row.get("postalCode")))
                .city(row.get("city"))
                .build();
    }

    // Given -----------------------------------------------------------------------

    @Given("an empty POS list")
    public void anEmptyPosList() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList).isEmpty();
    }

    // TODO: Add Given step for new scenario
    @Given("the following POS exist:")
    public void theFollowingPosExist(List<PosDto> posList) {
        createdPosList = createPos(posList);
        assertThat(createdPosList).hasSize(posList.size());
    }

    @When("I insert POS with the following elements")
    public void insertPosWithTheFollowingValues(List<PosDto> posList) {
        createdPosList = createPos(posList);
        assertThat(createdPosList).size().isEqualTo(posList.size());
    }

    // TODO: Add When step for new scenario
    @When("I update the POS with name {string} to have description {string}")
    public void iUpdateThePosWithNameToHaveDescription(String name, String newDescription) {
        // 1. in createdPosList find POS with name
        PosDto posToUpdate = createdPosList.stream()
                .filter(pos -> pos.name().equals(name))   // getName() not, name()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "POS with name " + name + " not found in createdPosList"));

        // 2. with new description create new PosDto (immutable, no setter )
        PosDto request =
                PosDto.builder()
                        .id(posToUpdate.id())               // getId() not, id()
                        .name(posToUpdate.name())
                        .description(newDescription)
                        .type(posToUpdate.type())
                        .campus(posToUpdate.campus())
                        .street(posToUpdate.street())
                        .houseNumber(posToUpdate.houseNumber())
                        .postalCode(posToUpdate.postalCode())
                        .city(posToUpdate.city())
                        .build();

        // 3. PUT /api/pos/{id} update call
        updatedPos =
                RestAssured.given()
                        .contentType("application/json")
                        .body(request)
                        .when()
                        .put("/api/pos/" + posToUpdate.id())
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(PosDto.class);
    }
    @Then("the POS list should contain the same elements in the same order")
    public void thePosListShouldContainTheSameElementsInTheSameOrder() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrderElementsOf(createdPosList);
    }

    // TODO: Add Then step for new scenario
    @Then("the POS with name {string} should have description {string}")
    public void thePosWithNameShouldHaveDescription(String name, String expectedDescription) {
        assertThat(updatedPos.name()).isEqualTo(name);
        assertThat(updatedPos.description()).isEqualTo(expectedDescription);

        /*

        List<PosDto> retrievedPosList = retrievePos();
        PosDto fromApi = retrievedPosList.stream()
                .filter(pos -> pos.name().equals(name))
                .findFirst()
                .orElseThrow();
        assertThat(fromApi.description()).isEqualTo(expectedDescription);
        */
    }
}
