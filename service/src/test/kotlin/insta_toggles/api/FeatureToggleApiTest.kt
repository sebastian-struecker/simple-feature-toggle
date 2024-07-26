package insta_toggles.api

import insta_toggles.Context
import insta_toggles.ContextName
import insta_toggles.FeatureToggle
import insta_toggles.api.models.CreateFeatureToggleRequest
import insta_toggles.api.models.FeatureToggleUpdateRequest
import insta_toggles.repository.FeatureTogglePanacheRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.response.Response
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito


@QuarkusTest
class FeatureToggleApiTest {

    @InjectMock
    lateinit var repositoryMock: FeatureTogglePanacheRepository

    @BeforeEach
    fun setupMocks() {
        Mockito.reset(repositoryMock)
    }

    @AfterEach
    fun resetMocks() {
        Mockito.reset(repositoryMock)
    }

    companion object {
        const val FEATURE_TOGGLE_URL: String = "/feature-toggle"
        const val FEATURE_TOGGLES_URL: String = "/feature-toggles"
    }

    @Test
    fun getAllActiveFeatures_context_invalid_test() {
        val feature = feature()
        Mockito.`when`(
            repositoryMock.getAllActive(ContextName.testing)
        ).thenReturn(
            Multi.createFrom().item(feature)
        )
        getAllActiveFeaturesRequest(context = "dummy").then().statusCode(200).body("size()", `is`(0))
    }

    @Test
    fun getAllActiveFeatures_missingApiKey_test() {
        getAllActiveFeaturesRequest(apiKey = null).then().statusCode(401)
    }

    @Test
    fun getAllActiveFeatures_wrongApiKey_test() {
        getAllActiveFeaturesRequest(apiKey = "fail").then().statusCode(401)
    }

    @Test
    fun getAllActiveFeatures_apiKeyDisabled_test() {
        QuarkusMock.installMockForType(
            ApiKeyFilter(
                listOf("test"), false
            ), ApiKeyFilter::class.java
        )
        getAllActiveFeaturesRequest(apiKey = "fail").then().statusCode(200)
    }

    @Test
    fun getAllActiveFeatures_testing_test() {
        val feature = feature()
        Mockito.`when`(
            repositoryMock.getAllActive(ContextName.testing)
        ).thenReturn(
            Multi.createFrom().item(feature)
        )
        getAllActiveFeaturesRequest().then().statusCode(200).body("size()", `is`(1))
    }

    @Test
    fun getAllActiveFeatures_production_test() {
        val feature = feature()
        Mockito.`when`(
            repositoryMock.getAllActive(ContextName.production)
        ).thenReturn(
            Multi.createFrom().item(feature)
        )
        getAllActiveFeaturesRequest(ContextName.production.toString()).then().statusCode(200).body("size()", `is`(1))
    }

    @Test
    fun getAll_unauthorized_test() {
        getAllRequest().then().statusCode(401)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun getAll_authorized_admin_test() {
        getAllRequest().then().statusCode(200)
    }

    @Test
    @TestSecurity(user = "viewer", roles = [DefaultRoles.VIEWER])
    fun getAll_authorized_viewer_test() {
        getAllRequest().then().statusCode(200)
    }

    @Test
    @TestSecurity(user = "release_manager", roles = [DefaultRoles.RELEASE_MANAGER])
    fun getAll_authorized_release_manager_test() {
        getAllRequest().then().statusCode(200)
    }

    @Test
    fun getById_unauthorized_test() {
        getByIdRequest(1).then().statusCode(401)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun getById_notFound_test() {
        getByIdRequest().then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun getById_authorized_admin_test() {
        val feature = feature()
        Mockito.`when`(repositoryMock.getById(1)).thenReturn(
            Uni.createFrom().item(feature)
        )
        getByIdRequest().then().statusCode(200).body("name", `is`(feature.name))
            .body("description", `is`(feature.description))
    }

    @Test
    @TestSecurity(user = "viewer", roles = [DefaultRoles.VIEWER])
    fun getById_authorized_viewer_test() {
        val feature = feature()
        Mockito.`when`(repositoryMock.getById(1)).thenReturn(
            Uni.createFrom().item(feature)
        )
        getByIdRequest().then().statusCode(200).body("name", `is`(feature.name))
            .body("description", `is`(feature.description))
    }

    @Test
    @TestSecurity(user = "release_manager", roles = [DefaultRoles.RELEASE_MANAGER])
    fun getById_authorized_release_manager_test() {
        val feature = feature()
        Mockito.`when`(repositoryMock.getById(1)).thenReturn(
            Uni.createFrom().item(feature)
        )
        getByIdRequest().then().statusCode(200).body("name", `is`(feature.name))
            .body("description", `is`(feature.description))
    }

    @Test
    fun create_unauthorized_test() {
        val request = CreateFeatureToggleRequest("key", "name", "description")
        createRequest(request).then().statusCode(401)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun create_authorized_admin_test() {
        val feature = feature()
        Mockito.`when`(repositoryMock.create(feature.key, feature.name, feature.description)).thenReturn(
            Uni.createFrom().item(feature)
        )
        val request = CreateFeatureToggleRequest(feature.key, feature.name, feature.description)
        createRequest(request).then().statusCode(200).body("name", `is`(request.name))
            .body("description", `is`(request.description))
    }

    @Test
    @TestSecurity(user = "viewer", roles = [DefaultRoles.VIEWER])
    fun create_unauthorized_viewer_test() {
        val request = CreateFeatureToggleRequest("key", "name", "description")
        createRequest(request).then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "release-manager", roles = [DefaultRoles.RELEASE_MANAGER])
    fun create_unauthorized_release_manager_test() {
        val request = CreateFeatureToggleRequest("key", "name", "description")
        createRequest(request).then().statusCode(403)
    }

    @Test
    fun partialUpdate_unauthorized_test() {
        val request = FeatureToggleUpdateRequest(null, null, null)
        partialUpdateRequest(request).then().statusCode(401)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun partialUpdate_notFound_test() {
        val request = FeatureToggleUpdateRequest(null, null, null)
        partialUpdateRequest(request).then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun partialUpdate_authorized_admin_test() {
        val feature = feature()
        val request = FeatureToggleUpdateRequest(
            "updated", "updated", null
        )
        Mockito.`when`(
            repositoryMock.update(
                feature.id, request
            )
        ).thenReturn(
            Uni.createFrom().item(feature.apply {
                name = "updated"
                description = "updated"
            })
        )
        partialUpdateRequest(request).then().statusCode(200).body("name", `is`(request.name))
            .body("description", `is`(request.description))
    }

    @Test
    @TestSecurity(user = "viewer", roles = [DefaultRoles.VIEWER])
    fun partialUpdate_unauthorized_viewer_test() {
        val request = FeatureToggleUpdateRequest(null, null, null)
        partialUpdateRequest(request).then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "release_manager", roles = [DefaultRoles.RELEASE_MANAGER])
    fun partialUpdate_unauthorized_release_manager_test() {
        val request = FeatureToggleUpdateRequest(null, null, null)
        partialUpdateRequest(request).then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun deleteById_authorized_admin_test() {
        Mockito.`when`(
            repositoryMock.removeById(1)
        ).thenReturn(
            Uni.createFrom().nullItem()
        )
        deleteByIdRequest().then().statusCode(200)
    }

    @Test
    @TestSecurity(user = "viewer", roles = [DefaultRoles.VIEWER])
    fun deleteById_unauthorized_viewer_test() {
        deleteByIdRequest().then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "release_manager", roles = [DefaultRoles.RELEASE_MANAGER])
    fun deleteById_unauthorized_release_manager_test() {
        deleteByIdRequest().then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "admin", roles = [DefaultRoles.ADMIN])
    fun deleteAll_authorized_admin_test() {
        Mockito.`when`(
            repositoryMock.removeAll()
        ).thenReturn(
            Uni.createFrom().nullItem()
        )
        deleteAllRequest().then().statusCode(200)
    }

    @Test
    @TestSecurity(user = "viewer", roles = [DefaultRoles.VIEWER])
    fun deleteAll_unauthorized_viewer_test() {
        deleteAllRequest().then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "release_manager", roles = [DefaultRoles.RELEASE_MANAGER])
    fun deleteAll_unauthorized_release_manager_test() {
        deleteAllRequest().then().statusCode(403)
    }

    private fun getAllActiveFeaturesRequest(context: String = "testing", apiKey: String? = "test"): Response {
        if (apiKey == null) {
            return given().`when`().get("$FEATURE_TOGGLES_URL/$context")
        }
        val headers = Headers.headers(Header("x-api-key", apiKey))
        return given().`when`().headers(headers).get("$FEATURE_TOGGLES_URL/$context")
    }

    private fun getAllRequest(): Response = given().`when`().get(FEATURE_TOGGLES_URL)

    private fun getByIdRequest(id: Long = 1): Response = given().`when`().get("$FEATURE_TOGGLE_URL/$id")

    private fun createRequest(request: CreateFeatureToggleRequest): Response =
        given().`when`().body(request).contentType(ContentType.JSON).post(FEATURE_TOGGLES_URL)

    private fun partialUpdateRequest(request: FeatureToggleUpdateRequest, id: Long = 1): Response =
        given().`when`().body(request).contentType(ContentType.JSON).patch("$FEATURE_TOGGLE_URL/$id")

    private fun deleteByIdRequest(id: Long = 1): Response = given().`when`().delete("$FEATURE_TOGGLE_URL/$id")

    private fun deleteAllRequest(): Response = given().`when`().delete(FEATURE_TOGGLES_URL)

    private fun feature() = FeatureToggle(
        1L, "key", "name", "description", listOf(
            Context(1, ContextName.testing.toString(), ContextName.testing.toString(), false),
            Context(2, ContextName.production.toString(), ContextName.production.toString(), false)
        )
    )

}
