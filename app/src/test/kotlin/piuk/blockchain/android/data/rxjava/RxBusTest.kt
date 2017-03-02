package piuk.blockchain.android.data.rxjava

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.*

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class RxBusTest : RxTest() {

    private lateinit var subject: RxBus

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = RxBus()
    }

    @Test
    @Throws(Exception::class)
    fun registerSingleObserver() {
        // Arrange
        val type = String::class.java
        // Act
        subject.register(type)
        // Assert
        subject.subjectsMap shouldContainKey type
        subject.subjectsMap.size equals 1
        subject.subjectsMap[type]!!.size equals 1
    }

    @Test
    @Throws(Exception::class)
    fun registerMultipleObserversOfSameType() {
        // Arrange
        val type = String::class.java
        // Act
        subject.register(type)
        subject.register(type)
        subject.register(type)
        // Assert
        subject.subjectsMap shouldContainKey type
        subject.subjectsMap.size equals 1
        subject.subjectsMap[type]!!.size equals 3
    }

    @Test
    @Throws(Exception::class)
    fun registerMultipleObserversOfDifferentTypes() {
        // Arrange
        val type0 = String::class.java
        val type1 = Integer::class.java
        val type2 = Double::class.java
        // Act
        subject.register(type0)
        subject.register(type1)
        subject.register(type2)
        // Assert
        subject.subjectsMap shouldContainKey type0
        subject.subjectsMap shouldContainKey type1
        subject.subjectsMap shouldContainKey type2
        subject.subjectsMap.size equals 3
        subject.subjectsMap[type0]!!.size equals 1
        subject.subjectsMap[type1]!!.size equals 1
        subject.subjectsMap[type2]!!.size equals 1
    }

    @Test
    @Throws(Exception::class)
    fun unregisterObserverOneRegistered() {
        // Arrange
        val type = String::class.java
        // Act
        val observable = subject.register(type)
        subject.unregister(type, observable)
        // Assert
        subject.subjectsMap shouldNotContainKey type
        subject.subjectsMap.size equals 0
    }

    @Test
    @Throws(Exception::class)
    fun unregisterObserverMultipleRegistered() {
        // Arrange
        val type = String::class.java
        // Act
        val observableToBeLeftRegistered = subject.register(type)
        val observableToBeUnregistered = subject.register(type)
        subject.unregister(type, observableToBeUnregistered)
        // Assert
        subject.subjectsMap shouldContainKey type
        subject.subjectsMap.size equals 1
        subject.subjectsMap[type]!!.size equals 1
        subject.subjectsMap[type]!![0] equals observableToBeLeftRegistered
    }

    @Test
    @Throws(Exception::class)
    fun unregisterObserverNoneRegistered() {
        // Arrange
        val type = String::class.java
        // Act
        val observable: Observable<String> = mock()
        subject.unregister(type, observable)
        // Assert
        subject.subjectsMap shouldNotContainKey type
        subject.subjectsMap.size equals 0
    }

    @Test
    @Throws(Exception::class)
    fun emitEventTypeRegistered() {
        // Arrange
        val type = String::class.java
        val value = "VALUE"
        // Act
        val testObserver = subject.register(type).test()
        subject.emitEvent(type, value)
        // Assert
        testObserver.assertNoErrors()
        testObserver.values().size equals 1
        testObserver.values()[0] equals value
    }

    @Test
    @Throws(Exception::class)
    fun emitEventTypeNotRegistered() {
        // Arrange
        val typeToRegister = Double::class.java
        val typeToEmit = String::class.java
        val value = "VALUE"
        // Act
        val testObserver = subject.register(typeToRegister).test()
        subject.emitEvent(typeToEmit, value)
        // Assert
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
    }

}