package com.saged.githubvault
import com.saged.githubvault.feature.github.GitHubPolicy
import org.junit.Assert.*
import org.junit.Test
class GitHubPolicyTest {
 @Test fun fixedOwner(){assertEquals("saged7",GitHubPolicy.OWNER);assertEquals("/repos/saged7/sageer",GitHubPolicy.root("sageer"));assertEquals("/repos/saged7/-",GitHubPolicy.root("-"))}
 @Test fun unicodePath(){assertEquals("مجلد/ملف.txt",GitHubPolicy.path("مجلد/ملف.txt"))}
 @Test fun emptyRoot(){assertEquals("",GitHubPolicy.path(""))}
 @Test fun rejectTraversal(){for(p in listOf("../x","/etc/passwd","a/../b","a/./b","a//b","a\\b","C:x","a\u0000"))reject{GitHubPolicy.path(p)}}
 @Test fun rejectRepoInjection(){for(r in listOf("../other","x/y","https://github.com/saged7", ".","..","","x?query"))reject{GitHubPolicy.repo(r)}}
 @Test fun shaFormat(){assertEquals("a".repeat(40),GitHubPolicy.sha("a".repeat(40)));reject{GitHubPolicy.sha("main")};reject{GitHubPolicy.sha("a".repeat(39))}}
 @Test fun tokenValidation(){val fake="github_pat_UNIT_TEST_NOT_A_REAL_TOKEN";assertEquals(fake,GitHubPolicy.token(fake));reject{GitHubPolicy.token("short")};reject{GitHubPolicy.token(fake+"\n")};reject{GitHubPolicy.token(fake+"\r\nHeader: value")};reject{GitHubPolicy.token(fake+" space")}}
 @Test fun boundedUpload(){assertEquals(90L*1024*1024,GitHubPolicy.UPLOAD_LIMIT)}
 private fun reject(block:()->Unit){try{block();fail("Expected rejection")}catch(_:IllegalArgumentException){}}
}
