package clarifai2.test;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.request.ClarifaiPaginatedRequest;
import clarifai2.api.request.ClarifaiRequest;
import clarifai2.exception.ClarifaiException;
import clarifai2.internal.InternalUtil;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public abstract class BaseClarifaiAPITest {

  @NotNull @Rule public TestName testName = new TestName();

  @NotNull final Logger logger = LoggerFactory.getLogger(getClass());

  @NotNull static final String METRO_NORTH_IMAGE_URL = "https://samples.clarifai.com/metro-north.jpg";
  @NotNull static final File METRO_NORTH_IMAGE_FILE = new File("../tests/assets/metro-north.jpg");
  @NotNull static final File KOTLIN_LOGO_IMAGE_FILE = new File("../tests/assets/image.png");

  @NotNull final String appID = EnvVar.CLARIFAI_APP_ID.value();
  @NotNull final String appSecret = EnvVar.CLARIFAI_APP_SECRET.value();
  @NotNull final String baseURL = EnvVar.CLARIFAI_API_BASE.value();


  @NotNull final ClarifaiClient client = new ClarifaiBuilder(appID, appSecret)
      .baseURL(baseURL)
      .client(new OkHttpClient.Builder()
          .connectTimeout(60, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .writeTimeout(60, TimeUnit.SECONDS)
          .addInterceptor(new HttpLoggingInterceptor(logger::debug).setLevel(HttpLoggingInterceptor.Level.BODY))
          .build()
      )
      .buildSync();

  @Before public void logTestNameBeginning() {
    logTestNameBlock("BEGIN TEST");
  }

  @After public void logTestNameEnd() {
    logTestNameBlock("END TEST");
  }

  private void logTestNameBlock(@NotNull String prefix) {
    logger.info("\n" +
        "#########################################################" + "\n" +
        "### " + prefix + ": " + testName.getMethodName() + "\n" +
        "#########################################################" + "\n"
    );
  }

  @Test(expected = ClarifaiException.class)
  public void testIncorrectAppID() {
    new ClarifaiBuilder("fewjiofjewiofjewiojwejfewoi", appSecret).buildSync();
  }

  @Test(expected = ClarifaiException.class)
  public void testIncorrectAppSecret() {
    new ClarifaiBuilder(appID, "fjweiojf2983fj203jf23ofj23ofj").buildSync();
  }

  <T> T assertSuccess(@NotNull ClarifaiPaginatedRequest<T> request) {
    return assertSuccess(request.getPage(1));
  }

  <T> T assertSuccess(@NotNull ClarifaiRequest<T> request) {
    final T result = request.executeSync().get();
    logger.info(result.toString());
    return result;
  }

  static void assertSuccessAsync(@NotNull ClarifaiPaginatedRequest<?> request) {
    assertSuccessAsync(request.getPage(1));
  }

  static <T> void assertSuccessAsync(@NotNull ClarifaiRequest<T> request) {
    request.executeAsync(new ClarifaiRequest.Callback<T>() {
      @Override public void onClarifaiResponseSuccess(@NotNull T o) {
      }

      @Override public void onClarifaiResponseUnsuccessful(int errorCode) {
        Assert.fail(InternalUtil.message("Clarifai response unsuccessful",
            "Request: " + request,
            "Error: " + errorCode
        ));
      }

      @Override public void onClarifaiResponseNetworkError(@NotNull IOException e) {
        Assert.fail(InternalUtil.message("Clarifai network error",
            "Exception: " + e
        ));
      }
    });
  }
}
