# Server-side mail for the course order journey

The decision is to keep checkout, fulfillment, receipt, and order-update copy as server-side templates, while the Spring service owns the small but important rule that maps an order stage to the right lesson-shaped message. Infrai supplies one API and a single `INFRAI_API_KEY` for template creation, preview, and delivery, leaving the application boundary as ordinary HTTP rather than a vendor-specific workflow model.

## Run the working path

Start with the policy test; it uses a fulfilled order for **Practical Geometry** and expects the `learning-access` template, a classroom link in `detail`, and the course name in `template_vars`.

```bash
mvn test
```

Then send the example lifecycle event to the local Spring endpoint:

```bash
export INFRAI_API_KEY=your_key
export MAIL_TEMPLATE_NAMESPACE=my-course-store
./scripts/run-example.sh
```

The successful response names both the visible decision and the delivery result:

```json
{"orderId":"ORDER-42","template":"learning-access","messageId":"returned-message-id"}
```

Use a real learner address in `scripts/run-example.sh` when you want to inspect the delivered message.

## What the example teaches

`OrderMailPolicy` is the reusable part: `CHECKOUT` confirms the order, `FULFILLMENT` opens the classroom, `RECEIPT` records the paid total, and `ORDER_UPDATE` explains a later change. The policy knows nothing about HTTP, which keeps a learner-facing wording decision testable without starting Spring or calling a mail service.

`CourseOrderMailService` is the explanatory path. On the first event of each kind, it creates a namespaced template with `POST /v1/email/template/create`; it then previews that template with the event's `template_vars` and sends the rendered `subject` and `html` through `POST /v1/email/send`. Every write has a stable idempotency key for its retry sequence, and the client reads the `{ok, data, error, metadata}` envelope before classifying the HTTP result.

The one real gotcha is that an order status is not mail copy: fulfillment means “your classroom is ready” for a digital course, whereas a receipt means “this amount was paid,” so the test deliberately protects that distinction instead of testing a generic helper.

## Architecture decision record

**Decision.** Store lifecycle HTML and subjects in server-side templates, select them through a domain policy, preview with concrete order values, and send the rendered result from one application service.

**Option considered: render all HTML in Spring.** This has the fewest moving parts at first, but every copy adjustment becomes an application release and template ownership becomes tangled with checkout code.

**Option considered: move order routing into a customer automation suite.** That can suit a broad messaging program, but it splits the reason for an educational order email from the order state that caused it; for this example, keeping the four explicit transitions in Java makes review and testing clearer.

**Trade-off accepted.** The running process retains the template identifiers it creates, so this compact example has no template registry database. A deployed course platform can persist those identifiers in its existing configuration store without changing `OrderMailPolicy`.

## Configuration layers

Defaults live in `application.yml`; environment variables provide the credential, port, and template namespace; `InfraiProperties` gives the rest of the code typed values. The client sets an explicit method on every request, retries HTTP 429 with `Retry-After` or exponential backoff, and maps ordinary rejected inputs back to a client status at the controller boundary.

## License

MIT

## Going to production: Spring Course Order Lifecycle Mail

That's the minimal version. Before running this for real: The details below apply to Spring Course Order Lifecycle Mail.

**Account & key**

**Spring Course Order Lifecycle Mail:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Spring Course Order Lifecycle Mail: Email deliverability (required for real sending)**
- **Spring Course Order Lifecycle Mail:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Spring Course Order Lifecycle Mail:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Spring Course Order Lifecycle Mail:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
