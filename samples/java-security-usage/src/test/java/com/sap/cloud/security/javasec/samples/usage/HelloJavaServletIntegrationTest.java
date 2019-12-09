package com.sap.cloud.security.javasec.samples.usage;

import com.sap.cloud.security.config.Environments;
import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.cf.CFConstants;
import com.sap.cloud.security.test.SecurityIntegrationTestRule;
import com.sap.cloud.security.xsuaa.http.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static com.sap.cloud.security.config.Service.XSUAA;
import static com.sap.cloud.security.config.cf.CFConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

public class HelloJavaServletIntegrationTest {

	private static Properties oldProperties;

	@ClassRule
	public static SecurityIntegrationTestRule rule = SecurityIntegrationTestRule.getInstance(XSUAA)
			.useServletServer()
			.addServlet(HelloJavaServlet.class, HelloJavaServlet.ENDPOINT);

	@BeforeClass
	public static void prepareTest() throws Exception {
		oldProperties = System.getProperties();
		System.setProperty(VCAP_SERVICES, IOUtils.resourceToString("/vcap.json", StandardCharsets.UTF_8));
		assertThat(Environments.getCurrent().getXsuaaConfiguration()).isNotNull();
		rule.setClientId(Environments.getCurrent().getXsuaaConfiguration().getClientId());
	}

	@AfterClass
	public static void restoreProperties() {
		System.setProperties(oldProperties);
	}

	@Test
	public void requestWithoutAuthorizationHeader_statusUnauthorized() throws IOException {
		HttpGet request = createGetRequest(null);
		try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
			assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED); // 401
		}
	}

	@Test
	public void requestWithEmptyAuthorizationHeader_statusUnauthorized() throws Exception {
		HttpGet request = createGetRequest("");
		try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
			assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED); // 401
		}
	}

	@Test
	public void request_withValidToken() throws IOException {
		HttpGet request = createGetRequest(rule.createToken().getBearerAccessToken());
		try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
			String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
		}
	}

	/** TODO as soon @ServletSecurity works

	@Test
	public void request_withValidTokenWithoutScopes_statusUnauthorized() throws IOException {
		HttpGet request = createGetRequest(rule.createToken().getBearerAccessToken());
		try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
			String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN); // 403
		}
	}

	@Test
	public void request_withValidToken_statusOk() throws IOException {
		Token tokenWithScopes = rule.getPreconfiguredJwtGenerator().withScopes(getGlobalScope("read")).createToken();
		HttpGet request = createGetRequest(tokenWithScopes.getBearerAccessToken());
		try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
			String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK); // 200
		}
	}

	**/

	private HttpGet createGetRequest(String bearerToken) {
		HttpGet httpGet = new HttpGet(rule.getServletServerUri() + HelloJavaServlet.ENDPOINT);
		if(bearerToken != null) {
			httpGet.setHeader(HttpHeaders.AUTHORIZATION, bearerToken);
		}
		return httpGet;
	}

	private String getGlobalScope(String scope) {
		String appId = Environments.getCurrent().getXsuaaConfiguration().getProperty(CFConstants.XSUAA.APP_ID);
		return appId + '.' + scope;
	}
}