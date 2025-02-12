
import java.lang.management.ManagementFactory

import java.lang.management.ThreadMXBean
import java.lang.management.ThreadInfo

ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean()

for (Long threadID : threadMXBean.getAllThreadIds()) {
    ThreadInfo info = threadMXBean.getThreadInfo(threadID)
    println('Thread name: ' + info.getThreadName())
    println('Thread State: ' + info.getThreadState())
    println("CPU time: ${threadMXBean.getThreadCpuTime(threadID)} ns")
  }
