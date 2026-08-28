#if canImport(Testing)
import Testing
import Flate2

@Suite("Flate2 Swift Export Tests")
struct Flate2ExportTests {
    @Test("Swift module loads")
    func testSwiftModuleLoads() {
        #expect(Bool(true), "Flate2 swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Flate2

final class Flate2ExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Flate2 swift module imported cleanly")
    }
}
#endif
