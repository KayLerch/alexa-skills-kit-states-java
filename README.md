[![Join the chat at https://gitter.im/alexa-skills-kit-states-java/Lobby](https://badges.gitter.im/alexa-skills-kit-states-java/Lobby.svg)](https://gitter.im/alexa-skills-kit-states-java/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven central](https://img.shields.io/badge/maven%20central-v1.0.2-orange.svg)](http://search.maven.org/#artifactdetails%7Cio.klerch%7Calexa-skills-kit-states-java%7C1.0.2%7Cjar)
![SonarQube Coverage](https://img.shields.io/badge/code%20coverage-82%25-green.svg)

__Reference projects__: The award-winning [Morse-Coder skill](http://alexaskillscentral.com/skills/2016/05/26/morse-coder/) heavily relies on the States SDK. To learn more about this SDK use [the open source of Morse Coder](https://github.com/KayLerch/alexa-morse-coder/) as a reference.

#Alexa States SDK for Java

This SDK is an extension to the Amazon Alexa Skills Kit for Java which
gives you a really convenient alternative of __persisting session state in a growing
number of persistence stores__ like DynamoDB, AWS S3 and AWS IoT. It is an abstraction layer
for reading state from and (permanently) saving state to either an Alexa session
or one of the aforementioned data stores. This also is your __framework for
building your own state handlers__ for any possible data store.

![Scopes in Alexa Skills Kit](/img/alexa-scopes-in-states-sdk.png)

Don't be scared by the complexity of that schema. Most of it is hidden for you when
using the SDK.

## How to use
Add below Maven dependency to your project.

```xml
<dependencies>
  ...
  <dependency>
    <groupId>io.klerch</groupId>
    <artifactId>alexa-skills-kit-states-java</artifactId>
    <version>1.0.2</version>
  </dependency>
  ...
</dependencies>
```

Depending on what features you use from this SDK you also need to add dependencies to certain AWS SDKs dedicated to S3, DynamoDb or IoT.

This SDK can __save you hundreds of lines of code__. See following examples where
you can see how to manage state of your prepared POJO model (referred as _AlexaStateModel_) or just single
values.

### Managing Alexa session state in _Alexa Session_
State is persisted throughout one client session.
```java
final AlexaStateHandler handler = new AlexaSessionStateHandler(session);
final User abby = handler.readModel(User.class, "Abby").orElse(handler.createModel(User.class, "Abby"));
abby.setPersonalHighscore(80);
abby.saveState();
handler.writeValue("overallHighscore", abby.getPersonalHighscore());
```
### Managing Alexa session state in a _AWS DynamoDB table_
State is persisted permanently per user.
```java
final AlexaStateHandler handler = new AWSDynamoStateHandler(session);
final User john = handler.readModel(User.class, "John").orElse(handler.createModel(User.class, "John"));
john.setPersonalHighscore(90);
john.saveState();
handler.writeValue("overallHighscore", john.getPersonalHighscore());
```
### Managing Alexa session state in a _AWS S3 bucket_
If you like to administer state objects in files why not using an S3 bucket?
```java
final AlexaStateHandler handler = new AWSS3StateHandler(session, "bucketName");
final User bob = handler.readModel(User.class, "Bob").orElse(handler.createModel(User.class, "Bob"));
bob.setPersonalHighscore(100);
bob.saveState();
handler.writeValue("overallHighscore", bob.getPersonalHighscore());
```

### Propagate Alexa session state to a _AWS IoT thing shadow_
You can not only use handlers to persist states but also to propagate it.
By propagating state to an AWS IoT thing shadow you interact with physical things easily
```java
final AlexaStateHandler handler = new AWSIoTStateHandler(session);
final User tim = handler.readModel(User.class, "Tim").orElse(handler.createModel(User.class, "Tim"));
tim.setPersonalHighscore(110);
tim.saveState();
handler.writeValue("overallHighscore", tim.getPersonalHighscore());
```

It is easy to __implement your own AlexaStateHandler__ so you can save
state in whatever you want to use.

Each model declares on its own what is saved and even can decide on what scope is
used to read and write attributes. That is how you can __not only save state per
user but also per application__ for e.g. managing the highscore of your game skill.

Now you will learn how to pimp your Alexa skill with permanent state capability
in minutes.

## 1) Prepare your POJO model class
This step is optional. If you just want to read/write single values to the store go on
with step 4).

For complex information schema in your skill you better __start organizing your state
in objects__. The above sample had the _User_-object. Think of a POJO with some
member fields.
1) Let your POJO derive from _AlexaStateModel_ and you are ready to go.
2) Tag members of your POJO whose state you want to save.
```java
public class User extends AlexaStateModel {
    @AlexaStateSave(Scope = AlexaScope.USER)
    private Integer personalHighscore;
    @AlexaStateSave(Scope = AlexaScope.SESSION)
    private Integer currentScore;
    // ...
}
```
Optionally you can give each member a scope so you can decide on the context
the value is saved. Where _personalHighscore_ is persisted per _USER_ on a permanent basis,
_currentScore_ will only be saved throughout one Alexa session.
Instead of white-listing members of your model you can also black-list them
if tagging the whole model as _AlexaStateSave_
```java
@AlexaStateSave(Scope = AlexaScope.APPLICATION)
public class QuizGame extends AlexaStateModel {
    private Integer highscore;
    private String highscorer;
    @AlexaStateIgnore
    private Integer level;
    // ...
}
```
Wow, there is the third scope _APPLICATION_ you can use to let
state of your models be valid throughout all users in all sessions. The
_highscore_ value will be shared amongst all users of your skill whereas
the _level_ is ignored and will not persist in your session.

![Scopes in Alexa Skills Kit extensions for state management](/img/alexa-scopes.png)

When serializing and deserializing models, the States SDK relies on [Jackson](http://wiki.fasterxml.com/JacksonHome)'s ObjectMapper.
That being said, you can use Json-properties, reference your own (de)serialization-logic and more in the _AlexaStateModels_.

## 2) Choose your _AlexaStateHandler_
Depending on where you want to save your model's states you can pick from
one of the following handlers:

The __AlexaSessionStateHandler__ persists state in the Alexa session JSON and
and is not capable of saving state in _USER_- or _APPLICATION_-scope.
```java
final AlexaStateHandler sh1 = new AlexaSessionStateHandler(session);
```

The __AWSS3StateHandler__ persists state in files in an AWS S3 bucket. It can be
constructed in different ways. All you have to provide is an S3 bucket. You maybe want to hand in an AWS client from
the AWS SDK in order to have set up your own credentials and AWS region. As
the handler also gets the Alexa session object, whatever is read from or written to S3
will be in your Alexa session as well. So you won't need to read out your
model state over and over again within one session.
```java
final AlexaStateHandler s3h1 = new AWSS3StateHandler(session, "bucketName");
final AlexaStateHandler s3h2 = new AWSS3StateHandler(session, new AmazonS3Client().withRegion(Regions.US_EAST_1), "bucketName");
```

The __AWSDynamoStateHandler__ persists state in items in a DynamoDB table. If
you don't give it a table to work with, the handler creates one for you. Once
more you could hand in an AWS client with custom configuration. As
the handler gets the Alexa session object, whatever is read from or written to the table
will be in your Alexa session as well. So you won't need to read out your
model state over and over again within one session.
```java
final AlexaStateHandler dyh1 = new AWSDynamoStateHandler(session);
final AlexaStateHandler dyh2 = new AWSDynamoStateHandler(session, "tableName");
final AlexaStateHandler dyh3 = new AWSDynamoStateHandler(session, new AmazonDynamoDBClient(), "tableName");
```

The __AWSIoTStateHandler__ persists state in a virtual representation of a
physical thing - in AWS IoT this is called a thing shadow. AWS IoT manages state
of that thing and automatically propagates state updates to the connected thing.
It also receives state updates from the thing which will persist in the shadow as well.
This handler can also read out that updated data and serialize it in your model.
```java
final AlexaStateHandler ioth1 = new AWSIoTStateHandler(session);
final AlexaStateHandler ioth1 = new AWSIoTStateHandler(session, new AWSIotClient(), new AWSIotDataClient());
```
## 3) Create an instance of your model
So you got your POJO model and also your preferred state handler. They now need
to get introduced to each other. The most convenient way is to instantiate
your model with help of the state handler. Of course you can construct your model
as you like and set the handler later on.
```java
final User bob = handler.createModel(User.class, "Bob");
final QuizGame game = handler.createModel(QuizGame.class);
```
There's a big difference between both lines because the first one gives
the model being created an identifier. This is how you can have multiple
models per scope and can address their state with the same Id later on.
The second line does not provide an Id causing this model to be a scope-wide
singleton. What in this case makes total sense as the _QuizGame_ is scoped
as _APPLICATION_ and is shared with all users of your skill. Moreover, the
second approach won't let you deal with identifiers.

## 4) Save state of your model or single value
Continuing from above lines we now assign some values to _bob_ and set a
new highscore in the _game_. But nothing will be persisted until you tell
your model to save its state. There are two alternatives of doing so:
```java
// save state from within your model
bob.setPersonalHighscore(100);
bob.saveState();
// save state with handler
game.setHighscore(100);
handler.writeModel(game);
```
The first approach does work because we introduced _bob_ to the handler
on construction. The second approach would even work for models which
were constructed without the handler. Feel free to introduce your model
to another handler with its _setHandler(AlexaStateHandler handler)_ at
any time. Let's say you want to save _bob_'s state in S3 and in DynamoDB.
```java
bob.withHandler(s3Handler).saveState();
// or like this
dynamoHandler.writeModel(bob);
```

You are not limited to POJO models. Since version 1.0.0 you can also write
single values to a store. You need to provide an id and a serializable value and
optionally the scope you want that value to be saved in (by default the scope is
SESSION)
```java
dynamoHandler.writeValue("mySessionStateKey", "myValue");
dynamoHandler.writeValue("myUserStateKey", 123, AlexaScope.USER);
```

Moreover, you can write multiple models or values at once. Depending on the
used handler you can leverage batch processing capabilities to enhance
write performance to e.g. DynamoDB. If you´re using handlers which are not
able to batch-process the following calls result in multiple single write
 transactions behind the scenes (the S3 handler does that for example):
```java
final AlexaStateObject obj1 = new AlexaStateObject("mySessionStateKey", "myValue");
final AlexaStateObject obj2 = new AlexaStateObject("myUserStateKey", 123, AlexaScope.USER);
dynamoHandler.writeValues(Arrays.asList(obj1, obj2));
// or write multiple models
dynamoHandler.writeModels(bob, john, abby);
```

## 5) Read state of your model or single value
So real Bob is leaving his Echo for a week. After some days he's asking
your skill again what's his personal highscore. As your skill is pimped with the State SDK
it just needs to read out _bob_ over the same handler it was saved back then.
```java
final Optional<User> bob = handler.readModel(User.class, "Bob");
if (bob.isPresent()) {
    final Integer bobsHighscore = bob.get().getPersonalHighscore();
}
```
He also wants to know the current highscore amongst all users as this could have changed meanwhile.
Remember the _QuizGame_ is persisted in _APPLICATION_-scope.
```java
final QuizGame game = handler.readModel(QuizGame.class).orElse(handler.createModel(QuizGame.class));
final Integer highscore = game.getHighscore();
}
```
Thanks to _Optional_'s of Java8 we can react with creating a new _QuizGame_
on not-existing in a fancy one-liner. We should have already done so when
we constructed the models in chapter 3. Constructing and saving a model with
or without and Id potentially overwrites an existing model in the store.
Take the last code-lines as a best-practice of constructing your models.

Assume we used a _AWSDynamoStateHandler_ or _AWSS3StateHandler_ for reading
out _bob_'s state and we want use it throughout the current session without getting
back to S3 or DynamoDB a second time. Well, ...
```java
new AlexaSessionStateHandler(session).writeModel(bob);
```
Next time you can read out _bob_ with _AlexaSessionStateHandler_ and not with
the handler of DynamoDB or S3.
```java
final AlexaStateHandler sh = new AlexaSessionStateHandler(session);
final AlexaStateHandler dyh = new AWSDynamoStateHandler(session);

final User bob = sh.readModel(User.class, "bob").orElse(dyh.readModel(User.class, "bob").orElse(dyh.createModel(User.class, "bob")));
```

Once again you can also read to single values from the store by giving its id. By default
the value is read from SESSION scope unless you provide the desired scope to read from.
```java
final Optional<AlexaStateObject> obj1 = sh.readValue("mySessionStateKey");
final Optional<AlexaStateObject> obj2 = sh.readValue("myUserStateKey", AlexaScope.USER);
```

You can also read multiple values at once by providing a list of ids the handler should
look after in the store. It returns a list of _AlexaStateObjects_ found in the store.
```java
final List<AlexaStateObject> obj = sh.readValues("mySessionStateKey", "myUserStateKey");
```

Also check out what the _exists_ methods can do for you. These methods check existence of
models or single values in a store.
```java
if (sh.exists("mySessionStateKey") || sh.exists(User.class, "Bob")) {
    // ...
};
```

## 6) Remove state of your model or single value
Of course you can delete the state of your model. Let's keep it short as I
think you already got it.
```java
// first alternative
bob.removeState();
// second alternative
handler.removeModel(bob);
// or if we haven't read out bob so far
handler.readModel(User.class, "Bob").ifPresent(bob -> bob.removeState());
```

Same goes for single values by using _removeValue_. Once again, you can also remove more than one model or
value wiht _removeValues_ or _removeModels_.

## See how it works
Putting it together, there's a lot you can do with these extensions in
regards to state management in your Alexa skill.
Get detailed information for this SDK in the [Javadocs](https://kaylerch.github.io/alexa-skills-kit-states-java/).

One last example. Running _userScored("Bob", 100)_
```java
void userScored(String player, Integer score) throws AlexaStateErrorException {
    final AlexaStateHandler handler = new AWSDynamoStateHandler(this.session);
    final User user = handler.readModel(User.class, player).orElse(handler.createModel(User.class, player));
    // check if last score is player's personal highscore
    if (user.getPersonalHighscore() < score) {
        user.setPersonalHighscore(score);
        user.saveState();
    }
    // check if last score is all-time highscore of the game
    final QuizGame game = handler.readModel(QuizGame.class).orElse(handler.createModel(QuizGame.class));
    if (game.getHighscore() < score) {
        game.setHighscore(score);
        game.setHighscorer(player);
        game.saveState();
     }
}
```
with nothing you to bring except for some credentials to your AWS account
gets you to

![Bob's state in DynamoDB table](/img/bob-in-dynamo.png)

Let's congrat Bob for beating the highscore ;)

You may have noticed that by default Alexa's userId is used to as key when storing user-scoped
model-state. This userId that comes in with every request to your skill, will change when
a user re-enables your skill. That's not ideal as he would lose his state when enabling your skill
with another Amazon account or disables and re-enables your skill with the same account. 
If you're using account-linking you may have your own userId which is independant from the skill enablement.
You can assign custom userIds to a handler (_setUserId(String)_, _withUserId(String)_) and it
will use this one when saving user-scoped model-state.