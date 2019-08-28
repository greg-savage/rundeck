/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package webhooks

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.plugins.webhook.WebhookDataImpl
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification


class WebhookControllerSpec extends Specification implements ControllerUnitTest<WebhookController> {

    def "post"() {
        given:
        controller.frameworkService = Mock(AuthContextProcessor)
        controller.webhookService = Mock(MockWebhookService)

        when:
        params.authtoken = "1234"
        controller.post()

        then:
        1 * controller.webhookService.getWebhookByToken(_) >> { new Webhook(name:"test",authToken: "1234")}
        1 * controller.frameworkService.getAuthContextForSubject(_) >> { new SubjectAuthContext(null, null) }
        1 * controller.frameworkService.authorizeProjectResourceAny(_,_,_,_) >> { return true }
        1 * controller.webhookService.processWebhook(_,_,_,_) >> { }
        response.text == '{"msg":"ok"}'
    }

    def "post fail when not authorized"() {
        given:
        controller.frameworkService = Mock(AuthContextProcessor) {
            getAuthContextForSubject(_) >> { new SubjectAuthContext(null,null) }
            authorizeApplicationResourceAny(_,_,_) >> { return false }
        }
        controller.webhookService = Mock(MockWebhookService)

        when:
        params.authtoken = "1234"
        controller.post()

        then:
        1 * controller.webhookService.getWebhookByToken(_) >> { new Webhook(name:"test",authToken: "1234")}
        0 * controller.webhookService.processWebhook(_,_,_,_)
        response.text == '{"err":"You are not authorized to perform this action"}'
    }

    interface MockWebhookService {
        Webhook getWebhookByToken(String token)
        void processWebhook(String pluginName, String pluginConfigJson, WebhookDataImpl data, UserAndRolesAuthContext context)
    }
}
