package com.mockarena.challenge.application;

import com.mockarena.challenge.api.ChallengeDtos.*;
import com.mockarena.challenge.domain.*;
import com.mockarena.challenge.infrastructure.ChallengeCompositionRepository;
import com.mockarena.challenge.question.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service public class ChallengeApplicationService {
 private final ChallengeRepository challenges; private final ChallengeVersionRepository versions; private final ChallengeCompositionRepository composition; private final QuestionCatalogClient catalog; private final DeterministicQuestionSelector selector; private final Clock clock=Clock.systemUTC();
 public ChallengeApplicationService(ChallengeRepository c,ChallengeVersionRepository v,ChallengeCompositionRepository p,QuestionCatalogClient q,DeterministicQuestionSelector s){challenges=c;versions=v;composition=p;catalog=q;selector=s;}
 @Transactional public ChallengeResponse create(CreateChallengeRequest request,UUID actor){
  if(request.selectionGroups()!=null&&request.selection()!=null)throw new IllegalArgumentException("Provide selectionGroups or selection, not both");Instant now=clock.instant();Challenge challenge=challenges.save(new Challenge(UUID.randomUUID(),actor,request.visibility(),now));
  if(request.selectionGroups()==null){if(request.selection()==null)throw new IllegalArgumentException("selectionGroups is required");RuleBasedSelection rule=legacyRule(request.selection());UUID seed=seed(request.title()+"|"+rule);List<QuestionCatalogEntry> selected=selector.select(catalog.resolve(rule),rule.requestedQuestionCount(),seed);ChallengeVersion version=versions.saveAndFlush(new ChallengeVersion(UUID.randomUUID(),challenge.id(),request.title(),rule,seed,now));composition.save(version.id(),selected);return ChallengeResponse.from(challenge,version,selected);}
  List<SelectionGroup> groups=groups(request.selectionGroups());UUID seed=seed(request.title()+"|"+groups);List<QuestionCatalogEntry> selected=resolve(groups,seed);ChallengeVersion version=versions.saveAndFlush(new ChallengeVersion(UUID.randomUUID(),challenge.id(),request.title(),groups,seed,now));composition.save(version.id(),selected);return ChallengeResponse.from(challenge,version,selected);
 }
 @Transactional public ChallengeResponse publish(UUID id,int number,PublishChallengeVersionRequest request){Challenge c=requireChallenge(id);ChallengeVersion v=requireVersion(id,number);assertVersion(c.version(),request.expectedChallengeVersion());assertVersion(v.version(),request.expectedVersion());if(v.status()!=ChallengeVersionStatus.DRAFT)throw new IllegalStateException("Only draft challenge versions can be published");List<QuestionCatalogEntry> selected=v.selectionGroups().isEmpty()?selector.select(catalog.resolve(ruleOf(v)),v.requestedQuestionCount(),v.selectionSeed()):resolve(v.selectionGroups(),v.selectionSeed());assertVersion(c.version(),request.expectedChallengeVersion());assertVersion(v.version(),request.expectedVersion());if(v.status()!=ChallengeVersionStatus.DRAFT)throw new IllegalStateException("Only draft challenge versions can be published");Instant now=clock.instant();composition.replace(v.id(),selected);v.publish(now);c.publish(v.id(),now);versions.saveAndFlush(v);challenges.saveAndFlush(c);return ChallengeResponse.from(c,v,selected);}
 @Transactional public ChallengeResponse retire(UUID id,int n,RetireChallengeVersionRequest r){Challenge c=requireChallenge(id);ChallengeVersion v=requireVersion(id,n);assertVersion(c.version(),r.expectedChallengeVersion());assertVersion(v.version(),r.expectedVersion());List<QuestionCatalogEntry> m=composition.findByChallengeVersionId(v.id());Instant now=clock.instant();v.retire(now);c.clearCurrentPublishedVersion(v.id(),now);versions.saveAndFlush(v);challenges.saveAndFlush(c);return ChallengeResponse.from(c,v,m);}
 private List<QuestionCatalogEntry> resolve(List<SelectionGroup> groups,UUID seed){
  Set<UUID> reserved=new HashSet<>();List<QuestionCatalogEntry> result=new ArrayList<>();
  Comparator<Ranked> ranking=Comparator.comparing(Ranked::rank).thenComparing(x->x.entry().questionId()).thenComparing(x->x.entry().questionVersionId());
  for(int i=0;i<groups.size();i++){
   SelectionGroup g=groups.get(i);Set<UUID> seen=new HashSet<>();
   PriorityQueue<Ranked> best=new PriorityQueue<>(g.requestedQuestionCount(),ranking.reversed());
   int eligibleUniqueCount=0;String cursor=null;
   do{
    QuestionCatalogClient.CatalogPage page=catalog.resolvePage(g,cursor);
    for(QuestionCatalogEntry e:page.entries())if(!reserved.contains(e.questionId())&&seen.add(e.questionId())){
     eligibleUniqueCount++;Ranked candidate=new Ranked(e,rank(seed,i,e));
     if(best.size()<g.requestedQuestionCount())best.add(candidate);
     else if(ranking.compare(candidate,best.peek())<0){best.poll();best.add(candidate);}
    }
    cursor=page.nextCursor();
   }while(cursor!=null);
   if(eligibleUniqueCount<g.requestedQuestionCount())throw new InsufficientQuestionsException(i,g.questionTypeCodes().getFirst(),g.requestedQuestionCount(),eligibleUniqueCount);
   List<QuestionCatalogEntry> selected=best.stream().sorted(ranking).map(Ranked::entry).toList();
   selected.forEach(e->reserved.add(e.questionId()));result.addAll(selected);
  }
  return result;
 }
 private static String rank(UUID seed,int group,QuestionCatalogEntry e){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((seed+":"+group+":"+e.questionId()+":"+e.questionVersionId()).getBytes(StandardCharsets.UTF_8)));}catch(Exception x){throw new IllegalStateException(x);}}
 private static List<SelectionGroup> groups(List<SelectionGroupRequest> input){if(input==null||input.isEmpty())throw new IllegalArgumentException("selectionGroups is required");List<SelectionGroup> out=input.stream().map(x->new SelectionGroup(tax(x.taxonomyAll()),upper(x.questionTypeCodes()),profiles(x.difficultyProfiles()),values(x.contentLocales()),upper(x.programmingLanguages()),x.requestedQuestionCount())).toList();Set<String> used=new HashSet<>();int mcq=0,coding=0,total=0;for(SelectionGroup g:out){if(g.questionTypeCodes().size()!=1)throw new IllegalArgumentException("Each selection group must contain exactly one question type");String type=g.questionTypeCodes().getFirst();if(!Set.of("MCQ","CODING").contains(type)||!used.add(type))throw new IllegalArgumentException("Unsupported or duplicate V1 question type");if(type.equals("MCQ")&&!g.programmingLanguages().isEmpty())throw new IllegalArgumentException("programmingLanguages are only valid for CODING");if(type.equals("MCQ"))mcq+=g.requestedQuestionCount();else coding+=g.requestedQuestionCount();total+=g.requestedQuestionCount();}if(mcq>100||coding>10||total<10||total>110)throw new IllegalArgumentException("Custom assessment composition limits were exceeded");return out;}
 private static UUID seed(String v){return UUID.nameUUIDFromBytes(v.getBytes(StandardCharsets.UTF_8));}private static RuleBasedSelection ruleOf(ChallengeVersion v){return new RuleBasedSelection(v.taxonomyAll(),v.questionTypeCodes(),v.difficultyProfiles(),v.contentLocales(),v.programmingLanguages(),v.requestedQuestionCount());}private Challenge requireChallenge(UUID id){return challenges.findById(id).orElseThrow(()->new NoSuchElementException("Challenge not found"));}private ChallengeVersion requireVersion(UUID id,int n){ChallengeVersion v=versions.findByChallengeIdAndVersionNumber(id,n).orElseThrow(()->new NoSuchElementException("Challenge version not found"));if(!v.challengeId().equals(id))throw new NoSuchElementException("Challenge version not found");return v;}private static void assertVersion(long a,long e){if(a!=e)throw new OptimisticLockException("Stale resource version");}
 private static RuleBasedSelection legacyRule(RuleBasedSelectionRequest x){return new RuleBasedSelection(tax(x.taxonomyAll()),upper(x.questionTypeCodes()),profiles(x.difficultyProfiles()),values(x.contentLocales()),upper(x.programmingLanguages()),x.requestedQuestionCount());}private static List<String> values(List<String>x){return x==null?List.of():x.stream().map(v->v.trim().toLowerCase(Locale.ROOT)).distinct().sorted().toList();}private static List<String> upper(List<String>x){return x==null?List.of():x.stream().map(v->v.trim().toUpperCase(Locale.ROOT)).distinct().sorted().toList();}private static List<TaxonomyAssignment> tax(List<TaxonomyAssignmentRequest>x){return x==null?List.of():x.stream().map(v->new TaxonomyAssignment(v.scheme().trim().toLowerCase(Locale.ROOT),v.code().trim().toLowerCase(Locale.ROOT))).distinct().sorted(Comparator.comparing(TaxonomyAssignment::scheme).thenComparing(TaxonomyAssignment::code)).toList();}private static List<DifficultyProfile> profiles(List<DifficultyProfileRequest>x){return x==null?List.of():x.stream().map(v->new DifficultyProfile(v.scheme().trim().toLowerCase(Locale.ROOT),v.code().trim().toUpperCase(Locale.ROOT))).distinct().sorted(Comparator.comparing(DifficultyProfile::scheme).thenComparing(DifficultyProfile::code)).toList();}
 private record Ranked(QuestionCatalogEntry entry,String rank){}
}
