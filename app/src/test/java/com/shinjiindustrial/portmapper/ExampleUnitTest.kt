package com.shinjiindustrial.portmapper

import org.junit.Assert.*
import org.junit.Test
import java.util.SortedSet
import java.util.TreeSet


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UPnPManagerTests {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    @Test
    fun addingAndReplaceRules()
    {
        var igdDevice = IGDDevice(null, null)

        // if compare returns equals then they are considered the same and will be replaced in the TreeSet
        class PortMapperComparator : Comparator<PortMapping> {
            override fun compare(p1: PortMapping, p2: PortMapping): Int {
                return p1.ExternalPort.compareTo(p2.ExternalPort)
            }
        }
        val ss = TreeSet<PortMapping>(PortMapperComparator())
        ss.add(PortMapping("","","",12345,12345,"",false,1234,"",100L))
        ss.add(PortMapping("test","","",12345,12345,"",false,1234,"",100L))
        ss.add(PortMapping("","","",12346,12346,"",false,1234,"",100L))
        ss.add(PortMapping("","","",12344,12344,"",false,1234,"",100L))

        for (pm in ss)
        {
            print(pm.ExternalPort)
        }
    }

    fun generateRule(externalPort : Int)
    {

    }
}