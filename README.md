# Link Contact to Account

## Purpose

`automation.Link_Contact_to_Account` runs for a Contact record and tries to link that Contact to an existing Account based on the Contact's Clinic value. If it finds one exact match, it also assigns that Account to every Deal associated with the Contact.

The function deliberately does **not** create Accounts, Contacts, or Deals. It only links records that already exist.

## How to use and test it

### Normal use

No one needs to run the function manually, it runs automatically when a new Contact is created.

To use it:

1. Create a Contact.
2. Enter the clinic name in the **Clinic** field.
3. Save the Contact.
4. The function checks the Account names in the background.

If it finds one exact Account name, it fills in the Contact's **Account Name** and updates any Deals linked to that Contact with the same Account.

### What the emails mean

| Email outcome | What to do |
| --- | --- |
| No Clinic value | Add a Clinic value to the Contact and save it. |
| No matches found | Check the Clinic spelling, then either correct it or choose/create the appropriate Account manually. |
| Possible matches | Review the Accounts listed in the email and choose the correct Account manually. |
| Multiple exact matches | The CRM contains duplicate Account names. Review the listed Accounts and choose the correct one manually. |

### Simple test procedure

Use test records where possible so that real Contacts and Deals are not changed accidentally.

1. Create an Account called, for example, `Test Clinic`.
2. Create a Contact and enter `Test Clinic` in its **Clinic** field.
3. Save the Contact.
4. Confirm the Contact's **Account Name** is now `Test Clinic`.
5. If the Contact has an associated Deal, confirm the Deal's **Account Name** was also updated.

## Processing flow

```text
Contact workflow starts
        |
        v
Get Contact by contactId
        |
        +-- Contact unavailable --> email notification, stop
        |
        v
Read Contact.Clinc
        |
        +-- Blank --> email notification, stop
        |
        v
Find exact Account-name matches
        |
        +-- One exact match ------> link Contact + related Deals
        |
        +-- Multiple exact matches -> email notification, stop
        |
        +-- No exact matches ------> look for word-order matches
                                      |
                                      +-- One or more --> email notification, stop
                                      |
                                      +-- None --------> email notification, stop
```

## Email notifications

The function sends emails only when intervention is needed:

| Situation | Changes made? | Email subject |
| --- | --- | --- |
| Contact cannot be retrieved | No | `Link Contact to Account` |
| Clinic is blank | No | `Link Contact to Account - Not Found` |
| No exact or word-order match | No | `Link Contact to Account - No Matches Found` |
| One or more word-order matches | No | `Link Contact to Account - Multiple Possible Matches` |
| Multiple exact matches | No | `Link Contact to Account - Multiple exact matches` |
| One exact match | Yes: Contact and related Deals | No email; the success-email block is commented out. |

## Important safety rules

- Only a single exact match updates CRM records.
- Possible matches are suggestions only.
- Multiple exact matches are not resolved automatically.
- The script does not create records.
