import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

driver = {
    ChromeOptions o = new ChromeOptions()
    o.addArguments('headless')
    new ChromeDriver(o)
}
